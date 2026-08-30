package me.grax.jbytemod.ui.lists.entries;

import me.grax.jbytemod.utils.InstrUtils;
import me.grax.jbytemod.utils.TextUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

public class LVPEntry {
    private ClassNode cn;
    private MethodNode mn;
    private LocalVariableNode lvn;
    private String text;

    public LVPEntry(ClassNode cn, MethodNode mn, LocalVariableNode lvn) {
        this.cn = cn;
        this.mn = mn;
        this.lvn = lvn;
        this.text = TextUtils.toHtml(TextUtils.toBold("#" + lvn.index) + " ");
        if (lvn.desc != null && !lvn.desc.isEmpty()) {
            this.text += InstrUtils.getDisplayType(lvn.desc, true) + " ";
        }
        this.text += TextUtils.addTag(TextUtils.escape(lvn.name), "font color=#995555");
    }

    public ClassNode getCn() {
        return cn;
    }

    public MethodNode getMn() {
        return mn;
    }

    @Override
    public String toString() {
        return text;
    }

    public LocalVariableNode getLvn() {
        return lvn;
    }
}
