package de.xbrowniecodez.jbytemod.decompiler;

import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.decompiler.Decompiler;
import me.grax.jbytemod.ui.DecompilerPanel;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ASMifierDecompiler extends Decompiler implements Opcodes {
    public ASMifierDecompiler(JByteMod jbm, DecompilerPanel dp) {
        super(jbm, dp);
    }

    @Override
    public String decompile(byte[] classBytes, MethodNode methodNode) {
        if (classBytes == null) {
            throw new IllegalArgumentException("Class byte array cannot be null");
        }

        ClassReader classReader = new ClassReader(classBytes);

        // if we have a methodNode is provided, process it individually
        if (methodNode != null) {
            return decompileMethodNode(classReader, methodNode);
        }

        // decompile the entire class if no MethodNode is specified
        return traceClass(classReader);
    }

    private String decompileMethodNode(ClassReader classReader, MethodNode methodNode) {
        // parse the original class into a ClassNode
        ClassNode originalClass = new ClassNode(ASM9);
        classReader.accept(originalClass, 0);

        // create a clone with only the specified MethodNode
        ClassNode methodOnlyClass = new ClassNode(ASM9);
        methodOnlyClass.name = originalClass.name;
        methodOnlyClass.access = originalClass.access;

        if (methodNode.name != null && methodNode.desc != null) {
            methodOnlyClass.methods.add(methodNode);
        } else {
            throw new IllegalArgumentException("Invalid MethodNode: name or descriptor is null");
        }

        // write the classNode with the single method to a byte array
        ClassWriter classWriter = new ClassWriter(0);
        methodOnlyClass.accept(classWriter);

        // create a classReader for the modified class
        ClassReader newReader = new ClassReader(classWriter.toByteArray());

        // use ASMifier to decompile
        return traceClass(newReader);
    }

    private String traceClass(ClassReader classReader) {
        StringWriter output = new StringWriter();
        PrintWriter printWriter = new PrintWriter(output);

        // create a TraceClassVisitor to produce ASM-style code
        TraceClassVisitor visitor = new TraceClassVisitor(null, new ASMifier(), printWriter);
        classReader.accept(visitor, 0);

        return formatASMifierOutput(output.toString());
    }

    private static String formatASMifierOutput(String asmOutput) {
        StringBuilder formattedOutput = new StringBuilder();
        int indentLevel = 0;

        for (String line : asmOutput.split("\n")) {
            String trimmed = line.trim();

            // adjust indentation for closing braces
            if (trimmed.endsWith("};") || trimmed.endsWith("}")) {
                indentLevel = Math.max(0, indentLevel - 1);
            }

            // apply indentation
            formattedOutput.append(repeat("    ", indentLevel)).append(trimmed).append("\n");

            // adjust indentation for opening braces
            if (trimmed.endsWith("{")) {
                indentLevel++;
            }
        }

        return formattedOutput.toString();
    }

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

}
