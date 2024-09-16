package me.grax.jbytemod.ui.graph;

import de.xbrowniecodez.jbytemod.Main;
import lombok.Getter;
import me.grax.jbytemod.analysis.block.Block;
import me.grax.jbytemod.analysis.decompiler.code.ast.Expression;
import me.grax.jbytemod.analysis.decompiler.struct.Conversion;
import me.grax.jbytemod.analysis.decompiler.struct.JVMStack;
import me.grax.jbytemod.analysis.decompiler.syntax.nodes.NodeList;
import me.grax.jbytemod.utils.InstrUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;

@Getter
public class BlockVertex {
    private final ArrayList<AbstractInsnNode> code;
    private final LabelNode label;
    private final int listIndex;
    private String text = null;
    private final Block block;
    private final MethodNode methodNode;
    private final ArrayList<BlockVertex> input = new ArrayList<>();
    private JVMStack leftOverStack;
    private boolean setupText;

    public BlockVertex(MethodNode methodNode, Block block, ArrayList<AbstractInsnNode> code, LabelNode label, int listIndex) {
        super();
        this.methodNode = methodNode;
        this.block = block;
        this.code = code;
        this.label = label;
        this.listIndex = listIndex;
    }

    public void addInput(BlockVertex v) {
        if (!input.contains(v)) {
            this.input.add(v);
        }
    }

    public void setupText() {
        if (setupText) {
            return;
        }
        text = "";
        if (Main.INSTANCE.getJByteMod().getOptions().get("decompile_graph").getBoolean()) {
            try {
                NodeList list = new NodeList();
                JVMStack inputStack = null;
                if (!input.isEmpty()) {
                    inputStack = input.get(0).getLeftOverStack();
                    assert (inputStack != null);
                }
                Conversion c = new Conversion(methodNode, list, inputStack);
                c.convert(block);
                leftOverStack = c.getStack();
                for (Expression e : list) {
                    text += e.toString() + "\n";
                }
            } catch (Exception e) {
                for (AbstractInsnNode ain : code) {
                    text += InstrUtils.toString(ain) + "\n";
                }
                text += "\n<i>";
                //text += ExceptionUtilities.getStackTraceString(e);
            }
        }
        if (text.trim().isEmpty()) {
            for (AbstractInsnNode ain : code) {
                text += InstrUtils.toString(ain) + "\n";
            }
        }
        setupText = true;
    }

    @Override
    public String toString() {
        if (text == null) {
            setupText();
        }
        return text;
    }
}