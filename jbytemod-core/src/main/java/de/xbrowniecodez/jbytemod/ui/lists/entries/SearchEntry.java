package de.xbrowniecodez.jbytemod.ui.lists.entries;

import lombok.Getter;
import lombok.Setter;
import me.grax.jbytemod.utils.InstrUtils;
import me.grax.jbytemod.utils.TextUtils;
import me.lpk.util.OpUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

@Getter
@Setter
public class SearchEntry {
    private ClassNode classNode;
    private MethodNode methodNode;
    private FieldNode fieldNode;
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

    public SearchEntry(ClassNode classNode, FieldNode fieldNode, String found) {
        this.classNode = classNode;
        this.fieldNode = fieldNode;
        this.found = found;
        this.text = TextUtils.toHtml(
                InstrUtils.getDisplayClass(classNode.name) + "." + TextUtils.escape(fieldNode.name) + " - " + TextUtils.addTag("\"" + found + "\"", "font color=#559955"));
    }

    public SearchEntry(ClassNode classNode, MethodNode methodNode, FieldInsnNode fin) {
        this(classNode, methodNode, fin.owner, fin.name, fin.desc, fin.getOpcode());
    }

    public SearchEntry(ClassNode classNode, MethodNode methodNode, MethodInsnNode min) {
        this(classNode, methodNode, min.owner, min.name, min.desc, min.getOpcode());
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