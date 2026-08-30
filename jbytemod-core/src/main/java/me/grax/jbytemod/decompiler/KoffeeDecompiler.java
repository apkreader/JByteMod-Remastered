package me.grax.jbytemod.decompiler;


import codes.som.anthony.koffee.disassembler.ClassDisassemblyKt;
import codes.som.anthony.koffee.disassembler.MethodDisassemblyKt;
import codes.som.anthony.koffee.disassembler.util.DisassemblyContext;
import codes.som.anthony.koffee.disassembler.util.SourceCodeGenerator;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.ui.DecompilerPanel;
import org.objectweb.asm.tree.MethodNode;

public class KoffeeDecompiler extends Decompiler {
    public KoffeeDecompiler(JByteMod jbm, DecompilerPanel dp) {
        super(jbm, dp);
    }

    @Override
    public String decompile(byte[] b, MethodNode mn) {
        SourceCodeGenerator sourceCodeGenerator = new SourceCodeGenerator();
        DisassemblyContext context = new DisassemblyContext(cn.name);
        if (mn == null) {
            return ClassDisassemblyKt.disassemble(cn);
        } else {
            MethodDisassemblyKt.disassembleMethod(mn, sourceCodeGenerator, context);
        }

        return sourceCodeGenerator.toString();
    }
}
