package me.grax.jbytemod.decompiler;

import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.ui.DecompilerPanel;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class Decompiler extends Thread {
    /**
     * Do not reload if we already know the output
     */
    public static ClassNode last;
    public static MethodNode lastMn;
    public static String lastOutput;
    protected JByteMod jbm;
    protected ClassNode cn;
    protected DecompilerPanel dp;
    private MethodNode mn;

    public Decompiler(JByteMod jbm, DecompilerPanel dp) {
        this.jbm = jbm;
        this.dp = dp;
    }

    public Decompiler setNode(ClassNode cn, MethodNode mn) {
        this.cn = cn;
        this.mn = mn;
        return this;
    }

    public Decompiler deleteCache() {
        last = null;
        return this;
    }

    @Override
    public final void run() {
        dp.setText("Loading...");
        if (cn == null) {
            dp.setText("ClassNode is null.");
            return;
        }
        dp.setText(lastOutput = this.decompile(cn, mn));
    }

    protected String decompile(ClassNode cn, MethodNode mn) {
        if (last != null && cn.equals(last)
                && ((lastMn == null && mn == null) || (mn != null && lastMn != null && mn.equals(lastMn)))) {
            // same node, same output
            return lastOutput;
        }
        last = cn;
        lastMn = mn;
        // do not regenerate anything here
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        try{
            return decompile(cw.toByteArray(), mn);
        }catch(Exception exception){
            return "Failed to decompile, reason: " + exception.getMessage();
        }

    }

    public abstract String decompile(byte[] b, MethodNode mn);
}
