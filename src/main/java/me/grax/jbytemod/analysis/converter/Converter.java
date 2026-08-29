package me.grax.jbytemod.analysis.converter;

import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import me.grax.jbytemod.analysis.block.Block;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Converter implements Opcodes {

    private static final boolean DEBUG = false;
    private ArrayList<AbstractInsnNode> nodes;
    private MethodNode mn;

    public Converter(MethodNode mn) {
        assert (mn.instructions != null && mn.instructions.size() > 0);
        this.mn = mn;
        this.nodes = new ArrayList<>(Arrays.asList(mn.instructions.toArray()));
    }

    public Converter(AbstractInsnNode[] array) {
        assert (array != null && array.length > 0);
        this.nodes = new ArrayList<>(Arrays.asList(array));
    }

    public ArrayList<Block> convert(boolean simplify, boolean removeRedundant, boolean skipDupeSwitches, int maxInputRemoveNonsense) {
        ArrayList<Block> blocks = new ArrayList<>();
        HashMap<AbstractInsnNode, Block> correspBlock = new HashMap<>();
        Block block = null;
        if (nodes.isEmpty()) {
            return blocks;
        }
        //detect block structure & last block insns
        for (AbstractInsnNode ain : nodes) {
            if (block == null) {
                block = new Block();
            }
            if (ain instanceof LabelNode) {
                block.setLabel((LabelNode) ain);
            }
            block.getNodes().add(ain);
            correspBlock.put(ain, block);
            //end blocks
            int op = ain.getOpcode();
            if (op >= IRETURN && op <= RETURN || ain instanceof JumpInsnNode || op == ATHROW || op == LOOKUPSWITCH || op == TABLESWITCH) {
                block.setEndNode(ain);
                blocks.add(block);
                block = null;
                continue;
            }
            //next because the label should have a new block
            //blocks that end without a jump
            if (ain.getNext() != null && (ain.getNext() instanceof LabelNode)) {
                block.setEndNode(ain.getNext());
                blocks.add(block);
                block = null;
                continue;
            }
        }
        for (Block b : blocks) {
            AbstractInsnNode end = b.getEndNode();
            //only opc where it continues
            if (end instanceof JumpInsnNode) {
                JumpInsnNode jin = (JumpInsnNode) end;
                ArrayList<Block> outputs = new ArrayList<>();
                if (!correspBlock.containsKey(jin.label)) {
                    throw new RuntimeException("label not visited");
                }
                Block blockAtLabel = correspBlock.get(jin.label);
                //blockAtLabel can be the same block!
                if (end.getOpcode() == GOTO) {
                    outputs.add(blockAtLabel);
                    b.setOutput(outputs);
                    blockAtLabel.getInput().add(b);
                } else {
                    //ifs have two outputs: either it jumps or not
                    outputs.add(blockAtLabel);
                    if (jin.getNext() == null) {
                        throw new RuntimeException("if has no next entry");
                    }
                    if (correspBlock.get(jin.getNext()) == b) {
                        throw new RuntimeException("next node is self?");
                    }
                    Block blockAfter = correspBlock.get(jin.getNext());
                    outputs.add(blockAfter);
                    b.setOutput(outputs);
                    blockAtLabel.getInput().add(b);
                    blockAfter.getInput().add(b);
                }
            } else if (end instanceof TableSwitchInsnNode) {
                ArrayList<Block> outputs = new ArrayList<>();
                TableSwitchInsnNode tsin = (TableSwitchInsnNode) end;
                if (tsin.dflt != null) {
                    Block blockAtDefault = correspBlock.get(tsin.dflt);
                    blockAtDefault.getInput().add(b);
                    outputs.add(blockAtDefault);
                }
                ArrayList<LabelNode> alreadyConnected = new ArrayList<>();
                for (LabelNode l : tsin.labels) {
                    if (skipDupeSwitches) {
                        if (alreadyConnected.contains(l))
                            continue;
                        alreadyConnected.add(l);
                    }
                    Block blockAtCase = correspBlock.get(l);
                    blockAtCase.getInput().add(b);
                    outputs.add(blockAtCase);
                }
                b.setOutput(outputs);
            } else if (end instanceof LookupSwitchInsnNode) {
                ArrayList<Block> outputs = new ArrayList<>();
                LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) end;
                if (lsin.dflt != null) {
                    Block blockAtDefault = correspBlock.get(lsin.dflt);
                    blockAtDefault.getInput().add(b);
                    outputs.add(blockAtDefault);
                }
                ArrayList<LabelNode> alreadyConnected = new ArrayList<>();
                for (LabelNode l : lsin.labels) {
                    if (skipDupeSwitches) {
                        if (alreadyConnected.contains(l))
                            continue;
                        alreadyConnected.add(l);
                    }
                    Block blockAtCase = correspBlock.get(l);
                    blockAtCase.getInput().add(b);
                    outputs.add(blockAtCase);
                }
                b.setOutput(outputs);
            } else if (end instanceof LabelNode) {
                if (!correspBlock.containsKey(end)) {
                    throw new RuntimeException("label not visited");
                }
                ArrayList<Block> outputs = new ArrayList<>();
                Block blockAtNext = correspBlock.get(end);
                outputs.add(blockAtNext);
                b.setOutput(outputs);
                blockAtNext.getInput().add(b);
            }
        }
        if (this.mn != null && this.mn.tryCatchBlocks != null) {
            for (TryCatchBlockNode tcb : this.mn.tryCatchBlocks) {
                Block handlerBlock = correspBlock.get(tcb.handler);
                if (handlerBlock != null) {
                    Block startBlock = correspBlock.get(tcb.start);
                    if (startBlock != null) {
                        if (!startBlock.getExceptions().contains(handlerBlock)) {
                            startBlock.getExceptions().add(handlerBlock);
                            if (!handlerBlock.getInput().contains(startBlock)) {
                                handlerBlock.getInput().add(startBlock);
                            }
                        }
                        String type = tcb.type != null ? tcb.type : "java/lang/Throwable";
                        String existing = startBlock.exceptionTypes.get(handlerBlock);
                        if (existing == null || !existing.contains(type)) {
                            startBlock.exceptionTypes.put(handlerBlock,
                                    existing == null ? type : existing + ", " + type);
                        }
                    }
                }
            }
        }
        Block first = correspBlock.get(nodes.get(0));
        assert (first != null);
        if (removeRedundant) {
            Set<Block> visited = new HashSet<>();
            removeNonsense(visited, blocks, first, maxInputRemoveNonsense);
            for (Block b : new ArrayList<>(blocks)) {
                if (b.getInput().isEmpty()) {
                    removeNonsense(visited, blocks, b, maxInputRemoveNonsense);
                }
            }
        }
        if (simplify) {
            Set<Block> visited = new HashSet<>();
            simplifyBlock(visited, blocks, first);
            for (Block b : new ArrayList<>(blocks)) {
                if (b.getInput().isEmpty()) {
                    simplifyBlock(visited, blocks, b);
                }
            }
        }
        if (DEBUG) {
            Set<Block> visited = new HashSet<>();
            calculateDepths(visited, blocks, first, 0);
            for (Block b : blocks) {
                if (b.getInput().isEmpty()) {
                    calculateDepths(visited, blocks, b, 0);
                }
            }
        }
        return blocks;
    }

    private void calculateDepths(Set<Block> visited, ArrayList<Block> blocks, Block b, int depth) {
        if (visited.contains(b)) {
            return;
        }
        visited.add(b);
        b.setDepth(depth);
        if (b.endsWithJump()) {
            if (b.getOutput().size() > 1) {
                if (b.getInput().size() <= 1) {
                    //handle if surrounding
                    Block blockAtLabel = b.getOutput().get(0);
                    Block blockAfter = b.getOutput().get(1);
                    calculateDepths(visited, blocks, blockAtLabel, depth);
                    calculateDepths(visited, blocks, blockAfter, depth + 1);
                } else {
                    //handle while surrounding
                    Block blockAtLabel = b.getOutput().get(0);
                    Block blockAfter = b.getOutput().get(1);
                    calculateDepths(visited, blocks, blockAfter, depth);
                    calculateDepths(visited, blocks, blockAtLabel, depth + 1);
                }
            }
        }
        for (Block output : b.getOutput()) {
            calculateDepths(visited, blocks, output, depth);
        }
        for (Block exception : b.getExceptions()) {
            calculateDepths(visited, blocks, exception, depth);
        }
    }

    private void removeNonsense(Set<Block> visited, ArrayList<Block> blocks, Block b, int maxInputRemoveNonsense) {
        if (visited.contains(b)) {
            return;
        }
        visited.add(b);
        if (b.endsWithJump()) {
            if (b.getInput().size() <= maxInputRemoveNonsense && b.getOutput().size() == 1) { //there could be more inputs but that might lead to a unreadable graph
                if (isJumpBlock(b)) {
                    Block output = b.getOutput().get(0);
                    for (Block input : b.getInput()) {
                        input.getOutput().remove(b);
                        input.getOutput().add(output);
                        output.getInput().add(input);
                    }
                    output.getInput().remove(b);
                }
            }
        }
        for (Block output : new ArrayList<>(b.getOutput())) {
            removeNonsense(visited, blocks, output, maxInputRemoveNonsense);
        }
        for (Block exception : new ArrayList<>(b.getExceptions())) {
            removeNonsense(visited, blocks, exception, maxInputRemoveNonsense);
        }
    }

    private boolean isJumpBlock(Block b) {
        for (AbstractInsnNode ain : b.getNodes()) {
            int type = ain.getType();
            if (type != AbstractInsnNode.LABEL && type != AbstractInsnNode.LINE && type != AbstractInsnNode.FRAME
                    && type != AbstractInsnNode.JUMP_INSN) {
                return false;
            }
        }
        return true;
    }

    private void simplifyBlock(Set<Block> simplified, ArrayList<Block> blocks, Block b) {
        if (simplified.contains(b)) {
            return;
        }
        simplified.add(b);
        while (true) {
            if (b.getOutput().size() == 1) {
                Block to = b.getOutput().get(0);
                //also optimizes unnecessary gotos
                if (to != b && blocks.contains(to) && to.getInput().size() == 1 && !isFirst(to)) {
                    assert (to.getInput().get(0) == b);
                    b.getNodes().addAll(to.getNodes());
                    b.setEndNode(to.getEndNode());
                    b.setOutput(new ArrayList<>(to.getOutput()));
                    for (Block output : b.getOutput()) {
                        output.getInput().remove(to);
                        if (!output.getInput().contains(b)) {
                            output.getInput().add(b);
                        }
                    }
                    for (Block exception : to.getExceptions()) {
                        if (!b.getExceptions().contains(exception)) {
                            b.getExceptions().add(exception);
                            exception.getInput().add(b);
                        }
                        exception.getInput().remove(to);

                        String type = to.exceptionTypes.get(exception);
                        if (type != null) {
                            String existing = b.exceptionTypes.get(exception);
                            if (existing == null || !existing.contains(type)) {
                                b.exceptionTypes.put(exception, existing == null ? type : existing + ", " + type);
                            }
                        }
                    }
                    blocks.remove(to);
                    continue;
                }
            }
            break;
        }
        for (Block output : b.getOutput()) {
            simplifyBlock(simplified, blocks, output);
        }
        for (Block exception : b.getExceptions()) {
            simplifyBlock(simplified, blocks, exception);
        }
    }

    private boolean isFirst(Block to) {
        LabelNode ln = to.getLabel();
        if (ln != null && BytecodeUtils.getLabelIndex(ln) == 0) {
            return true;
        }
        return false;
    }
}
