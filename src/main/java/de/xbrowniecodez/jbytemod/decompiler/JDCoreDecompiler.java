package de.xbrowniecodez.jbytemod.decompiler;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.decompiler.Decompiler;
import me.grax.jbytemod.ui.DecompilerPanel;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;

import org.objectweb.asm.tree.MethodNode;

import java.util.Objects;

public class JDCoreDecompiler extends Decompiler {

    public JDCoreDecompiler(JByteMod jbm, DecompilerPanel dp) {
        super(jbm, dp);
    }

    Loader loader = new Loader() {
        @Override
        public byte[] load(String internalName) {
            return BytecodeUtils.getClassNodeBytes(Main.INSTANCE.getJByteMod().getJarArchive().getClasses().get(internalName));
        }

        @Override
        public boolean canLoad(String internalName) {
            return Objects.nonNull(Main.INSTANCE.getJByteMod().getJarArchive().getClasses().get(internalName));
        }
    };

    Printer printer = new Printer() {
        private static final String TAB = "   ";
        private static final String NEWLINE = "\n";

        private int indentationCount = 0;
        private final StringBuilder sb = new StringBuilder();

        @Override public String toString() { return sb.toString(); }

        @Override public void start(int maxLineNumber, int majorVersion, int minorVersion) {}
        @Override public void end() {}

        @Override public void printText(String text) { sb.append(text); }
        @Override public void printNumericConstant(String constant) { sb.append(constant); }
        @Override public void printStringConstant(String constant, String ownerInternalName) { sb.append(constant); }
        @Override public void printKeyword(String keyword) { sb.append(keyword); }
        @Override public void printDeclaration(int type, String internalTypeName, String name, String descriptor) { sb.append(name); }
        @Override public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) { sb.append(name); }

        @Override public void indent() { this.indentationCount++; }
        @Override public void unindent() { this.indentationCount--; }

        @Override public void startLine(int lineNumber) { for (int i=0; i<indentationCount; i++) sb.append(TAB); }
        @Override public void endLine() { sb.append(NEWLINE); }
        @Override public void extraLine(int count) { while (count-- > 0) sb.append(NEWLINE); }

        @Override public void startMarker(int type) {}
        @Override public void endMarker(int type) {}
    };


    public String decompile(byte[] b, MethodNode mn) {
        ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
        try {
            decompiler.decompile(loader, printer, cn.name);
        } catch (Exception e) {
            return e.toString();
        }
        return printer.toString();
    }
}
