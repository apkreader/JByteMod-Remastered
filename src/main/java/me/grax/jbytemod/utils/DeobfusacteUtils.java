package me.grax.jbytemod.utils;

import com.javadeobfuscator.deobfuscator.analyzer.AnalyzerResult;
import com.javadeobfuscator.deobfuscator.analyzer.MethodAnalyzer;
import com.javadeobfuscator.deobfuscator.analyzer.frame.LdcFrame;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

import com.javadeobfuscator.deobfuscator.analyzer.frame.*;
import com.javadeobfuscator.deobfuscator.utils.Utils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DeobfusacteUtils {
    // Some of them are from https://github.com/java-deobfuscator or https://github.com/ItzSomebody/Radon/

    public static void fixSignature(Map<String, ClassNode> classes){
        classes.values().forEach(DeobfusacteUtils::fixSignature);
    }

    public static void fixSignature(ClassNode classNode) {
        if (classNode.signature != null) {
            try {
                CheckClassAdapter.checkClassSignature(classNode.signature);
            } catch (IllegalArgumentException IAE) {
                classNode.signature = null;
            } catch (Throwable x) {
                x.printStackTrace();
            }
        }

        classNode.methods.forEach(methodNode -> {
            if (methodNode.signature != null) {
                try {
                    CheckClassAdapter.checkMethodSignature(methodNode.signature);
                } catch (IllegalArgumentException IAE) {
                    methodNode.signature = null;
                } catch (Throwable x) {
                    x.printStackTrace();
                }
            }
        });

        classNode.fields.forEach(fieldNode -> {
            if (fieldNode.signature != null) {
                try {
                    CheckClassAdapter.checkFieldSignature(fieldNode.signature);
                } catch (IllegalArgumentException IAE) {
                    fieldNode.signature = null;
                } catch (Throwable x) {
                    x.printStackTrace();
                }
            }
        });

    }

    public static void removeLineNumber(Map<String, ClassNode> classes){
        classes.values().forEach(DeobfusacteUtils::removeLineNumber);
    }

    public static void removeLineNumber(ClassNode classNode) {
        classNode.methods.forEach(methodNode -> {
            Iterator<AbstractInsnNode> it = methodNode.instructions.iterator();
            while (it.hasNext()) {
                if (it.next() instanceof LineNumberNode) {
                    it.remove();
                }
            }
        });
    }

    public static void removeLocalVariable(Map<String, ClassNode> classes) {
        classes.values().forEach(DeobfusacteUtils::removeLocalVariable);
    }

    public static void removeLocalVariable(ClassNode classNode){
        classNode.methods.forEach(methodNode -> {
            methodNode.localVariables = null;
        });
    }

    public static void removeSyntheticBridge(Map<String, ClassNode> classes){
        classes.values().forEach(DeobfusacteUtils::removeSyntheticBridge);
    }

    public static void removeSyntheticBridge(ClassNode classNode){
        classNode.access &= ~(Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);

        classNode.methods.forEach(methodNode -> {
            methodNode.access &= ~(Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);
        });

        classNode.fields.forEach(fieldNode -> {
            fieldNode.access &= ~(Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);
        });
    }

    public static void removeIllegalVarargs(Map<String, ClassNode> classes){
        classes.values().forEach(DeobfusacteUtils::removeIllegalVarargs);
    }

    public static void removeIllegalVarargs(ClassNode classNode) {
        classNode.methods.forEach(methodNode -> {
            Type[] args = Type.getArgumentTypes(methodNode.desc);
            if (args.length > 0 && args[args.length - 1].getSort() != Type.ARRAY) {
                methodNode.access &= ~Opcodes.ACC_VARARGS;
            }
        });
    }

    public static int foldConstant(Map<String, ClassNode> classes){

        AtomicInteger folded = new AtomicInteger();
        classes.values().forEach(classNode -> {
            classNode.methods.stream().filter(methodNode -> methodNode.instructions.getFirst() != null).forEach(methodNode -> {
                try{
                    int start;
                    do {
                        start = folded.get();
                        AnalyzerResult result = MethodAnalyzer.analyze(classNode, methodNode);

                        Map<AbstractInsnNode, InsnList> replacements = new HashMap<>();

                        for (int i = 0; i < methodNode.instructions.size(); i++) {
                            AbstractInsnNode ain = methodNode.instructions.get(i);
                            opcodes:
                            switch (ain.getOpcode()) {
                                case IADD:
                                case ISUB:
                                case IMUL:
                                case IDIV:
                                case IREM:
                                case ISHL:
                                case ISHR:
                                case IUSHR:
                                case IXOR: {
                                    List<Frame> frames = result.getFrames().get(ain);
                                    if (frames == null) {
                                        break;
                                    }
                                    Set<Integer> results = new HashSet<>();
                                    for (Frame frame0 : frames) {
                                        MathFrame frame = (MathFrame) frame0;
                                        if (frame.getTargets().size() != 2) {
                                            throw new RuntimeException("weird: " + frame);
                                        }
                                        Frame top = frame.getTargets().get(0);
                                        Frame bottom = frame.getTargets().get(1);
                                        if (top instanceof LdcFrame && bottom instanceof LdcFrame) {
                                            int bottomValue = ((Number) ((LdcFrame) bottom).getConstant()).intValue();
                                            int topValue = ((Number) ((LdcFrame) top).getConstant()).intValue();
                                            if (ain.getOpcode() == IADD) {
                                                results.add(bottomValue + topValue);
                                            } else if (ain.getOpcode() == IMUL) {
                                                results.add(bottomValue * topValue);
                                            } else if (ain.getOpcode() == IREM) {
                                                results.add(bottomValue % topValue);
                                            } else if (ain.getOpcode() == ISUB) {
                                                results.add(bottomValue - topValue);
                                            } else if (ain.getOpcode() == IDIV) {
                                                results.add(bottomValue / topValue);
                                            } else if (ain.getOpcode() == ISHL) {
                                                results.add(bottomValue << topValue);
                                            } else if (ain.getOpcode() == ISHR) {
                                                results.add(bottomValue >> topValue);
                                            } else if (ain.getOpcode() == IUSHR) {
                                                results.add(bottomValue >>> topValue);
                                            } else if (ain.getOpcode() == IXOR) {
                                                results.add(bottomValue ^ topValue);
                                            }
                                        } else {
                                            break opcodes;
                                        }
                                    }
                                    if (results.size() == 1) {
                                        InsnList replacement = new InsnList();
                                        replacement.add(new InsnNode(POP2)); // remove existing args from stack
                                        replacement.add(new LdcInsnNode(results.iterator().next()));
                                        replacements.put(ain, replacement);
                                        folded.getAndIncrement();
                                    }
                                    break;
                                }
                                case TABLESWITCH: {
                                    List<Frame> frames = result.getFrames().get(ain);
                                    if (frames == null) {
                                        // wat
                                        break;
                                    }
                                    Set<Integer> results = new HashSet<>();
                                    Set<LdcFrame> resultFrames = new HashSet<>();
                                    for (Frame frame0 : frames) {
                                        SwitchFrame frame = (SwitchFrame) frame0;
                                        if (frame.getSwitchTarget() instanceof LdcFrame) {
                                            resultFrames.add((LdcFrame)frame.getSwitchTarget());
                                            results.add(((Number) ((LdcFrame) frame.getSwitchTarget()).getConstant()).intValue());
                                        } else {
                                            break opcodes;
                                        }
                                    }
                                    if(results.size() > 1)
                                    {
                                        //Impossible "infinite switch"
                                        Iterator<LdcFrame> itr = resultFrames.iterator();
                                        while(itr.hasNext())
                                        {
                                            LdcFrame ldcFrame = itr.next();
                                            AbstractInsnNode ldcNode = result.getMapping().get(ldcFrame);
                                            for(LabelNode label : ((TableSwitchInsnNode)ain).labels)
                                                if(label.getNext() != null && label.getNext().equals(ldcNode))
                                                {
                                                    results.remove(Utils.getIntValue(ldcNode));
                                                    itr.remove();
                                                }
                                        }
                                    }
                                    if (results.size() == 1) {
                                        TableSwitchInsnNode tsin = ((TableSwitchInsnNode) ain);
                                        int cst = results.iterator().next();
                                        LabelNode target = (cst < tsin.min || cst > tsin.max) ? tsin.dflt : tsin.labels.get(cst - tsin.min);
                                        InsnList replacement = new InsnList();
                                        replacement.add(new InsnNode(POP)); // remove existing args from stack
                                        replacement.add(new JumpInsnNode(GOTO, target));
                                        replacements.put(ain, replacement);
                                        folded.getAndIncrement();
                                    }
                                    break;
                                }
                                case IFGE:
                                case IFGT:
                                case IFLE:
                                case IFLT:
                                case IFNE:
                                case IFEQ: {
                                    List<Frame> frames = result.getFrames().get(ain);
                                    if (frames == null) {
                                        // wat
                                        break;
                                    }
                                    Set<Boolean> results = new HashSet<>();
                                    for (Frame frame0 : frames) {
                                        JumpFrame frame = (JumpFrame) frame0;
                                        if (frame.getComparators().get(0) instanceof LdcFrame) {
                                            int value = ((Number) ((LdcFrame) frame.getComparators().get(0)).getConstant()).intValue();
                                            if (ain.getOpcode() == IFGE) {
                                                results.add(value >= 0);
                                            } else if (ain.getOpcode() == IFGT) {
                                                results.add(value > 0);
                                            } else if (ain.getOpcode() == IFLE) {
                                                results.add(value <= 0);
                                            } else if (ain.getOpcode() == IFLT) {
                                                results.add(value < 0);
                                            } else if (ain.getOpcode() == IFNE) {
                                                results.add(value != 0);
                                            } else if (ain.getOpcode() == IFEQ) {
                                                results.add(value == 0);
                                            } else {
                                                throw new RuntimeException();
                                            }
                                        } else if(frame.getComparators().get(0) instanceof LocalFrame
                                                && frame.getComparators().size() == 1
                                                && frame.getComparators().get(0).getChildren().size() == 2
                                                && frame.getComparators().get(0).getChildren().get(0).getOpcode() ==
                                                frame.getComparators().get(0).getOpcode() - 33
                                                && ((LocalFrame)frame.getComparators().get(0)).getValue() instanceof LdcFrame) {
                                            //ldc - store - load - if
                                            LdcFrame cst = (LdcFrame)((LocalFrame)frame.getComparators().get(0)).getValue();
                                            int value = ((Number)cst.getConstant()).intValue();
                                            if (ain.getOpcode() == IFGE) {
                                                results.add(value >= 0);
                                            } else if (ain.getOpcode() == IFGT) {
                                                results.add(value > 0);
                                            } else if (ain.getOpcode() == IFLE) {
                                                results.add(value <= 0);
                                            } else if (ain.getOpcode() == IFLT) {
                                                results.add(value < 0);
                                            } else if (ain.getOpcode() == IFNE) {
                                                results.add(value != 0);
                                            } else if (ain.getOpcode() == IFEQ) {
                                                results.add(value == 0);
                                            } else {
                                                throw new RuntimeException();
                                            }
                                        } else {
                                            break opcodes;
                                        }
                                    }
                                    if (results.size() == 1) {
                                        InsnList replacement = new InsnList();
                                        replacement.add(new InsnNode(POP)); // remove existing args from stack
                                        if (results.iterator().next()) {
                                            replacement.add(new JumpInsnNode(GOTO, ((JumpInsnNode) ain).label));
                                        }
                                        replacements.put(ain, replacement);
                                        folded.getAndIncrement();
                                    }
                                    break;
                                }
                                case IF_ICMPLE:
                                case IF_ICMPGT:
                                case IF_ICMPGE:
                                case IF_ICMPLT:
                                case IF_ICMPNE:
                                case IF_ICMPEQ: {
                                    List<Frame> frames = result.getFrames().get(ain);
                                    if (frames == null) {
                                        // wat
                                        break;
                                    }
                                    Set<Boolean> results = new HashSet<>();
                                    for (Frame frame0 : frames) {
                                        JumpFrame frame = (JumpFrame) frame0;
                                        if (frame.getComparators().get(0) instanceof LdcFrame && frame.getComparators().get(1) instanceof LdcFrame) {
                                            int topValue = ((Number) ((LdcFrame) frame.getComparators().get(0)).getConstant()).intValue();
                                            int bottomValue = ((Number) ((LdcFrame) frame.getComparators().get(1)).getConstant()).intValue();
                                            if (ain.getOpcode() == IF_ICMPNE) {
                                                results.add(bottomValue != topValue);
                                            } else if (ain.getOpcode() == IF_ICMPEQ) {
                                                results.add(bottomValue == topValue);
                                            } else if (ain.getOpcode() == IF_ICMPLT) {
                                                results.add(bottomValue < topValue);
                                            } else if (ain.getOpcode() == IF_ICMPGE) {
                                                results.add(bottomValue >= topValue);
                                            } else if (ain.getOpcode() == IF_ICMPGT) {
                                                results.add(bottomValue > topValue);
                                            } else if (ain.getOpcode() == IF_ICMPLE) {
                                                results.add(bottomValue <= topValue);
                                            } else {
                                                throw new RuntimeException();
                                            }
                                        } else {
                                            break opcodes;
                                        }
                                    }
                                    if (results.size() == 1) {
                                        InsnList replacement = new InsnList();
                                        replacement.add(new InsnNode(POP2)); // remove existing args from stack
                                        if (results.iterator().next()) {
                                            replacement.add(new JumpInsnNode(GOTO, ((JumpInsnNode) ain).label));
                                        }
                                        replacements.put(ain, replacement);
                                        folded.getAndIncrement();
                                    }
                                    break;
                                }
                                case DUP: {
                                    List<Frame> frames = result.getFrames().get(ain);
                                    if (frames == null) {
                                        // wat
                                        break;
                                    }
                                    Set<Object> results = new HashSet<>();
                                    for (Frame frame0 : frames) {
                                        DupFrame frame = (DupFrame) frame0;
                                        if (frame.getTargets().get(0) instanceof LdcFrame) {
                                            results.add(((LdcFrame) frame.getTargets().get(0)).getConstant());
                                        } else {
                                            break opcodes;
                                        }
                                    }
                                    if (results.size() == 1) {
                                        Object val = results.iterator().next();
                                        InsnList replacement = new InsnList();
                                        if (val == null) {
                                            replacement.add(new InsnNode(ACONST_NULL));
                                        } else {
                                            replacement.add(new LdcInsnNode(val));
                                        }
                                        replacements.put(ain, replacement);
                                        folded.getAndIncrement();
                                    }
                                    break;
                                }
                                case POP:
                                case POP2: {
                                    List<Frame> frames = result.getFrames().get(ain);
                                    if (frames == null) {
                                        // wat
                                        break;
                                    }
                                    Set<AbstractInsnNode> remove = new HashSet<>();
                                    for (Frame frame0 : frames) {
                                        PopFrame frame = (PopFrame) frame0;
                                        if (frame.getRemoved().get(0) instanceof LdcFrame && (ain.getOpcode() != POP2 || frame.getRemoved().size() == 2 && frame.getRemoved().get(1) instanceof LdcFrame)) {
                                            for (Frame deletedFrame : frame.getRemoved()) {
                                                if (deletedFrame.getChildren().size() > 1) {
                                                    // ldc -> ldc -> swap -> pop = we can't even
                                                    break opcodes;
                                                }
                                                remove.add(result.getMapping().get(deletedFrame));
                                            }
                                        } else {
                                            if(frame.getRemoved().size() == 1)
                                            {
                                                //Load + pop
                                                Frame removed = frame.getRemoved().get(0);
                                                if(removed.getChildren().size() > 1 && removed.getChildren().indexOf(frame) - 1 >= 0
                                                        && removed.getChildren().get(removed.getChildren().indexOf(frame) - 1) instanceof LocalFrame
                                                        && removed.getChildren().get(removed.getChildren().indexOf(frame) - 1).getOpcode() >= ILOAD
                                                        && removed.getChildren().get(removed.getChildren().indexOf(frame) - 1).getOpcode() <= ALOAD)
                                                    remove.add(result.getMapping().get(removed.getChildren().get(removed.getChildren().indexOf(frame) - 1)));
                                                else
                                                    break opcodes;
                                            }else if(frame.getRemoved().size() == 2)
                                            {
                                                //Load + load + pop2
                                                Frame removed1 = frame.getRemoved().get(0);
                                                Frame removed2 = frame.getRemoved().get(1);
                                                if(removed1.equals(removed2) && removed1.getChildren().size() > 2 && removed1.getChildren().indexOf(frame) - 2 >= 0
                                                        && removed1.getChildren().get(removed1.getChildren().indexOf(frame) - 1) instanceof LocalFrame
                                                        && removed1.getChildren().get(removed1.getChildren().indexOf(frame) - 1).getOpcode() >= ILOAD
                                                        && removed1.getChildren().get(removed1.getChildren().indexOf(frame) - 1).getOpcode() <= ALOAD
                                                        && removed1.getChildren().get(removed1.getChildren().indexOf(frame) - 2) instanceof LocalFrame
                                                        && removed1.getChildren().get(removed1.getChildren().indexOf(frame) - 2).getOpcode() >= ILOAD
                                                        && removed1.getChildren().get(removed1.getChildren().indexOf(frame) - 2).getOpcode() <= ALOAD)
                                                {
                                                    //Previous instruction loads the same thing (expected children: load, load, pop2)
                                                    remove.add(result.getMapping().get(removed1.getChildren().get(removed1.getChildren().indexOf(frame) - 1)));
                                                    remove.add(result.getMapping().get(removed1.getChildren().get(removed1.getChildren().indexOf(frame) - 2)));
                                                }else if(removed1.getChildren().size() > 1 && removed2.getChildren().size() > 1
                                                        && removed1.getChildren().get(removed1.getChildren().indexOf(frame) - 1) instanceof LocalFrame
                                                        && removed2.getChildren().get(removed2.getChildren().indexOf(frame) - 1) instanceof LocalFrame)
                                                {
                                                    //Previous instruction is "load" and it loads different things
                                                    remove.add(result.getMapping().get(removed1.getChildren().get(removed1.getChildren().indexOf(frame) - 1)));
                                                    remove.add(result.getMapping().get(removed2.getChildren().get(removed2.getChildren().indexOf(frame) - 1)));
                                                }else
                                                    break opcodes;
                                            }else
                                                break opcodes;
                                        }
                                    }
                                    for (AbstractInsnNode insn : remove) {
                                        replacements.put(insn, new InsnList());
                                        replacements.put(ain, new InsnList());
                                        folded.getAndIncrement();
                                    }
                                    break;
                                }
                                default:
                                    break;
                            }
                        }

                        replacements.forEach((ain, replacement) -> {
                            methodNode.instructions.insertBefore(ain, replacement);
                            methodNode.instructions.remove(ain);
                        });
                    } while (start != folded.get());
                }catch (Exception i){

                }
            });
        });

        return folded.get();
    }

    public static int rearrangeGoto(Map<String, ClassNode> classes) {
        AtomicInteger counter = new AtomicInteger();
        classes.values().forEach(classNode -> {
            classNode.methods.stream().filter(methodNode -> methodNode.instructions.getFirst() != null).forEach(methodNode -> {
                Set<LabelNode> never = new HashSet<>();
                boolean modified;
                outer:
                do {
                    modified = false;
                    Map<LabelNode, Integer> jumpCount = new HashMap<>();
                    for (int i = 0; i < methodNode.instructions.size(); i++) {
                        AbstractInsnNode node = methodNode.instructions.get(i);
                        if (node instanceof JumpInsnNode) {
                            JumpInsnNode cast = (JumpInsnNode) node;
                            jumpCount.merge(cast.label, 1, Integer::sum);
                        } else if (node instanceof TableSwitchInsnNode) {
                            TableSwitchInsnNode cast = (TableSwitchInsnNode) node;
                            jumpCount.merge(cast.dflt, 1, Integer::sum);
                            cast.labels.forEach(l -> jumpCount.merge(l, 1, Integer::sum));
                        } else if (node instanceof LookupSwitchInsnNode) {
                            LookupSwitchInsnNode cast = (LookupSwitchInsnNode) node;
                            jumpCount.merge(cast.dflt, 1, Integer::sum);
                            cast.labels.forEach(l -> jumpCount.merge(l, 1, Integer::sum));
                        }
                    }
                    if (methodNode.tryCatchBlocks != null) {
                        methodNode.tryCatchBlocks.forEach(tryCatchBlockNode -> {
                            jumpCount.put(tryCatchBlockNode.start, 999);
                            jumpCount.put(tryCatchBlockNode.end, 999);
                            jumpCount.put(tryCatchBlockNode.handler, 999);
                        });
                    }
                    never.forEach(n -> jumpCount.put(n, 999));

                    for (int i = 0; i < methodNode.instructions.size(); i++) {
                        AbstractInsnNode node = methodNode.instructions.get(i);
                        if (node.getOpcode() == Opcodes.GOTO) {
                            JumpInsnNode cast = (JumpInsnNode) node;
                            if (jumpCount.get(cast.label) == 1) {
                                AbstractInsnNode next = cast.label;
                                AbstractInsnNode prev = Utils.getPrevious(next);
                                if (prev != null) {
                                    boolean ok = Utils.isTerminating(prev);
                                    while (next != null) {
                                        if (next == node) {
                                            ok = false;
                                        }
                                        if (methodNode.tryCatchBlocks != null) {
                                            for (TryCatchBlockNode tryCatchBlock : methodNode.tryCatchBlocks) {
                                                int start = methodNode.instructions.indexOf(tryCatchBlock.start);
                                                int mid = methodNode.instructions.indexOf(next);
                                                int end = methodNode.instructions.indexOf(tryCatchBlock.end);
                                                if (start <= mid && mid < end) {
                                                    // it's not ok if we're relocating the basic block outside the try-catch block
                                                    int startIndex = methodNode.instructions.indexOf(node);
                                                    if (startIndex < start || startIndex >= end) {
                                                        ok = false;
                                                    }
                                                }
                                            }
                                        }
                                        if (next != cast.label && jumpCount.getOrDefault(next, 0) > 0) {
                                            ok = false;
                                        }
                                        if (!ok) {
                                            break;
                                        }
                                        if (Utils.isTerminating(next)) {
                                            break;
                                        }
                                        next = next.getNext();
                                    }
                                    next = cast.label.getNext();
                                    if (ok) {
                                        List<AbstractInsnNode> remove = new ArrayList<>();
                                        while (next != null) {
                                            remove.add(next);
                                            if (Utils.isTerminating(next)) {
                                                break;
                                            }
                                            next = next.getNext();
                                        }
                                        InsnList list = new InsnList();
                                        remove.forEach(methodNode.instructions::remove);
                                        remove.forEach(list::add);
                                        methodNode.instructions.insert(node, list);
                                        methodNode.instructions.remove(node);
                                        modified = true;
                                        counter.incrementAndGet();
                                        continue outer;
                                    }
                                }
                            }
                        }
                    }
                } while (modified);
            });
        });
        return counter.get();
    }

    public static int mergeTrapHandler(Map<String, ClassNode> classes) {
        AtomicInteger redudantTraps = new AtomicInteger();
        classes.values().forEach(classNode -> {
            classNode.methods.stream().filter(methodNode -> methodNode.instructions.getFirst() != null).forEach(methodNode -> {
                if (methodNode.tryCatchBlocks != null && !methodNode.tryCatchBlocks.isEmpty()) {
                    Map<List<String>, List<TryCatchBlockNode>> merge = new HashMap<>();
                    Set<LabelNode> handled = new HashSet<>();
                    outer:
                    for (TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
                        if (!handled.add(tryCatchBlockNode.handler)) {
                            continue;
                        }
                        List<String> insns = new ArrayList<>(); // yes I know it's string because asm doesn't do equals
                        loop:
                        for (AbstractInsnNode now = tryCatchBlockNode.handler; ; ) {
                            if (now.getType() != AbstractInsnNode.LABEL && now.getType() != AbstractInsnNode.FRAME && now.getType() != AbstractInsnNode.LINE) {
                                // todo need some way of comparing insns
//                                int oldindex = now.index;
//                                now.index = 0;
//                                insns.add(Utils.prettyprint(now));
//                                now.index = oldindex;
                                switch (now.getOpcode()) {
                                    case RETURN:
                                    case ARETURN:
                                    case IRETURN:
                                    case FRETURN:
                                    case DRETURN:
                                    case LRETURN:
                                    case ATHROW:
                                        // done!
                                        break loop;
                                }
                                if (Utils.isTerminating(now) || now instanceof JumpInsnNode) {
                                    // not gonna worry about branching handlers for now
                                    continue outer;
                                }
                            }
                            now = now.getNext();
                        }

                        merge.computeIfAbsent(insns, key -> new ArrayList<>()).add(tryCatchBlockNode);
                    }

                    merge.forEach((insns, handlers) -> {
                        if (handlers.size() > 1) {
                            for (TryCatchBlockNode t : handlers) {
                                t.handler = handlers.get(0).handler;
                                redudantTraps.incrementAndGet();
                            }
                        }
                    });
                }
            });
        });
        return redudantTraps.get();
    }

    public static int removeUnconditionalSwitch(Map<String, ClassNode> classes) {
        AtomicInteger counter = new AtomicInteger();

        classes.values().forEach(classNode -> {
            MethodNode clinit = classNode.methods.stream().filter(mn -> mn.name.equals("<clinit>")).findFirst().orElse(null);
            if (clinit != null) {
                Map<LabelNode, LabelNode> mapping = new HashMap<>();
                InsnList insns = clinit.instructions;
                for (int i = 0; i < insns.size(); i++) {
                    AbstractInsnNode node = insns.get(i);
                    if (node instanceof LabelNode) {
                        mapping.put((LabelNode) node, (LabelNode) node);
                    }
                }
                for (int i = 0; i < insns.size(); i++) {
                    AbstractInsnNode node = insns.get(i);
                    int prev = Utils.iconstToInt(node.getOpcode());
                    if (prev == Integer.MIN_VALUE) {
                        if (node.getOpcode() == Opcodes.BIPUSH || node.getOpcode() == Opcodes.SIPUSH) {
                            prev = ((IntInsnNode) node).operand;
                        }
                    }
                    if (prev == Integer.MIN_VALUE) {
                        if (node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof Integer) {
                            prev = (Integer) ((LdcInsnNode) node).cst;
                        }
                    }
                    if (prev != Integer.MIN_VALUE) {
                        AbstractInsnNode next = Utils.getNextFollowGoto(node);
                        if (next instanceof TableSwitchInsnNode) {
                            TableSwitchInsnNode cast = (TableSwitchInsnNode) next;
                            int index = prev - cast.min;
                            LabelNode go = null;
                            if (index >= 0 && index < cast.labels.size()) {
                                go = cast.labels.get(index);
                            } else {
                                go = cast.dflt;
                            }
                            InsnList replace = new InsnList();
                            replace.add(new JumpInsnNode(Opcodes.GOTO, go));
                            insns.insertBefore(node, replace);
                            insns.remove(node);
                            counter.incrementAndGet();
                        }
                    }
                }
            }
        });

        classes.values().forEach(classNode -> {
            MethodNode clinit = classNode.methods.stream().filter(mn -> mn.name.equals("<clinit>")).findFirst().orElse(null);
            if (clinit != null) {
                Map<LabelNode, LabelNode> mapping = new HashMap<>();
                InsnList insns = clinit.instructions;
                for (int i = 0; i < insns.size(); i++) {
                    AbstractInsnNode node = insns.get(i);
                    if (node instanceof LabelNode) {
                        mapping.put((LabelNode) node, (LabelNode) node);
                    }
                }
                for (int i = 0; i < insns.size(); i++) {
                    AbstractInsnNode node = insns.get(i);
                    int prev = Utils.iconstToInt(node.getOpcode());
                    if (prev == Integer.MIN_VALUE) {
                        if (node.getOpcode() == Opcodes.BIPUSH || node.getOpcode() == Opcodes.SIPUSH) {
                            prev = ((IntInsnNode) node).operand;
                        }
                    }
                    if (prev == Integer.MIN_VALUE) {
                        if (node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof Integer) {
                            prev = (Integer) ((LdcInsnNode) node).cst;
                        }
                    }
                    if (prev != Integer.MIN_VALUE) {
                        AbstractInsnNode next = Utils.getNextFollowGoto(node);
                        if (next.getOpcode() == Opcodes.SWAP) {
                            next = Utils.getNextFollowGoto(next);
                            if (next.getOpcode() == Opcodes.INVOKESTATIC) {
                                AbstractInsnNode methodNode = next;
                                next = Utils.getNextFollowGoto(next);
                                if (next.getOpcode() == Opcodes.SWAP) {
                                    next = Utils.getNextFollowGoto(next);
                                    if (next instanceof TableSwitchInsnNode) {
                                        TableSwitchInsnNode cast = (TableSwitchInsnNode) next;
                                        int index = prev - cast.min;
                                        LabelNode go = null;
                                        if (index >= 0 && index < cast.labels.size()) {
                                            go = cast.labels.get(index);
                                        } else {
                                            go = cast.dflt;
                                        }
                                        InsnList replace = new InsnList();
                                        replace.add(methodNode.clone(null));
                                        replace.add(new JumpInsnNode(Opcodes.GOTO, go));
                                        insns.insertBefore(node, replace);
                                        insns.remove(node);
                                        counter.incrementAndGet();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
        return counter.get();
    }

    public static void removeIllegalInvisibleAnnotations(Map<String, ClassNode> classes){
        classes.values().forEach(DeobfusacteUtils::removeIllegalInvisibleAnnotations);
    }

    public static void removeIllegalInvisibleAnnotations(ClassNode classNode){
        if(!hasInvisibleAnnotations(classNode)) return;

        // Check the class first.
        for (AnnotationNode invisibleAnnotation : classNode.invisibleAnnotations) {
            if (invisibleAnnotation.desc.contains("\n")) classNode.invisibleAnnotations = null;
        }

        // Now we need to check the methods.
        classNode.methods.forEach(methodNode -> {
            if(hasInvisibleAnnotations(methodNode)){
                methodNode.invisibleAnnotations.forEach(annotationNode -> {
                    if(annotationNode.desc.contains("\n")) methodNode.invisibleAnnotations = null;
                });
            }
        });

        // Finally, we need to check the fields.
        classNode.fields.forEach(fieldNode -> {
            if(hasInvisibleAnnotations(fieldNode)){
                fieldNode.invisibleAnnotations.forEach(annotationNode -> {
                    if(annotationNode.desc.contains("\n")) fieldNode.invisibleAnnotations = null;
                });
            }
        });
    }

    private static boolean hasInvisibleAnnotations(ClassNode classNode) {
        return classNode.invisibleAnnotations != null && !classNode.invisibleAnnotations.isEmpty();
    }

    private static boolean hasInvisibleAnnotations(MethodNode methodNode) {
        return methodNode.invisibleAnnotations != null && !methodNode.invisibleAnnotations.isEmpty();
    }

    private static boolean hasInvisibleAnnotations(FieldNode fieldNode) {
        return fieldNode.invisibleAnnotations != null && !fieldNode.invisibleAnnotations.isEmpty();
    }



    private static boolean hasAnnotations(ClassNode classNode) {
        return (classNode.visibleAnnotations != null && !classNode.visibleAnnotations.isEmpty())
                || (classNode.invisibleAnnotations != null && !classNode.invisibleAnnotations.isEmpty());
    }

    private static boolean hasAnnotations(MethodNode methodNode) {
        return (methodNode.visibleAnnotations != null && !methodNode.visibleAnnotations.isEmpty())
                || (methodNode.invisibleAnnotations != null && !methodNode.invisibleAnnotations.isEmpty());
    }

    private static boolean hasAnnotations(FieldNode fieldNode) {
        return (fieldNode.visibleAnnotations != null && !fieldNode.visibleAnnotations.isEmpty())
                || (fieldNode.invisibleAnnotations != null && !fieldNode.invisibleAnnotations.isEmpty());
    }
}
