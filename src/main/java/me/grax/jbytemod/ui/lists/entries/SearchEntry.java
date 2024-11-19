package me.grax.jbytemod.ui.lists.entries;

import lombok.Getter;
import lombok.Setter;
import me.grax.jbytemod.utils.InstrUtils;
import me.grax.jbytemod.utils.TextUtils;
import me.lpk.util.OpUtils;
import org.objectweb.asm.tree.*;

@Getter
@Setter
public class SearchEntry {
    private ClassNode classNode;
    private MethodNode methodNode;
    private String found;
    private String text;

    /**
     * Construct prototype entry
     */
    public SearchEntry() {
        this.text = " ";
    }

    public SearchEntry(ClassNode classNode, MethodNode methodNode, String found) {
        this.classNode = classNode;
        this.methodNode = methodNode;
        this.found = found;
        this.text = TextUtils.toHtml(
                InstrUtils.getDisplayClass(classNode.name) + "." + TextUtils.escape(methodNode.name) + " - " + TextUtils.addTag("\"" + found + "\"", "font color=#559955"));
    }

    public SearchEntry(ClassNode classNode, MethodNode methodNode, FieldInsnNode fin) {
        this(classNode, methodNode, fin.owner, fin.name, fin.desc, fin.getOpcode());
    }

    public SearchEntry(ClassNode classNode, MethodNode methodNode, MethodInsnNode min) {
        this(classNode, methodNode, min.owner, min.name, min.desc, min.getOpcode());
    }

    public SearchEntry(ClassNode classNode, MethodNode methodNode, InvokeDynamicInsnNode idn) {
        this.classNode = classNode;
        this.methodNode = methodNode;
        this.found = idn.name + idn.desc;
        this.text = TextUtils.toHtml(
                InstrUtils.getDisplayClass(classNode.name) + "." + TextUtils.escape(methodNode.name) + " - "
                        + TextUtils.toBold("invokedynamic") + " " + TextUtils.escape(idn.name)
                        + "(" + InstrUtils.getDisplayArgs(TextUtils.escape(idn.desc)) + ")");
    }

    public SearchEntry(ClassNode classNode, MethodNode methodNode, String owner, String name, String desc, int opcode) {
        this.classNode = classNode;
        this.methodNode = methodNode;
        this.found = owner + "." + name + desc;
        this.text = TextUtils.toHtml(InstrUtils.getDisplayClass(classNode.name) + "." + TextUtils.escape(methodNode.name) + " - "
                + TextUtils.toBold(OpUtils.getOpcodeText(opcode).toLowerCase()) + " " + InstrUtils.getDisplayClassRed(owner) + "." + TextUtils.escape(name)
                + "(" + InstrUtils.getDisplayArgs(TextUtils.escape(desc)) + ")");
    }

    @Override
    public String toString() {
        return text;
    }
}
