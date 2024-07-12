package de.xbrowniecodez.jbytemod.decompiler;

import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.decompiler.Decompiler;
import me.grax.jbytemod.ui.DecompilerPanel;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ASMifierDecompiler extends Decompiler {
    public ASMifierDecompiler(JByteMod jbm, DecompilerPanel dp) {
        super(jbm, dp);
    }

    @Override
    public String decompile(byte[] b, MethodNode mn) {
        ClassReader cr = new ClassReader(b);
        StringWriter out = new StringWriter();
        cr.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(
                out)), 0);
        return out.toString();
    }
}
