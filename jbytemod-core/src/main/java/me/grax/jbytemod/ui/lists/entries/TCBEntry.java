package me.grax.jbytemod.ui.lists.entries;

import me.grax.jbytemod.utils.InstrUtils;
import me.grax.jbytemod.utils.TextUtils;
import me.lpk.util.OpUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class TCBEntry {
    private ClassNode cn;
    private MethodNode mn;
    private TryCatchBlockNode tcbn;
    private String text;

    public TCBEntry(ClassNode cn, MethodNode mn, TryCatchBlockNode tcbn) {
        this.cn = cn;
        this.mn = mn;
        this.tcbn = tcbn;
        this.text = TextUtils.toHtml(
                (tcbn.type != null ? InstrUtils.getDisplayType(tcbn.type, true) : TextUtils.addTag("Null type", "font color=" + InstrUtils.primColor.getString())) + ": label " + OpUtils.getLabelIndex(tcbn.start) + " -> label "
                        + OpUtils.getLabelIndex(tcbn.end) + " handler: label " + (tcbn.handler == null ? "null" : OpUtils.getLabelIndex(tcbn.handler)));
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

    public TryCatchBlockNode getTcbn() {
        return tcbn;
    }
}
