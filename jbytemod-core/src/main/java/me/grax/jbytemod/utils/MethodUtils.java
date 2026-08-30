package me.grax.jbytemod.utils;

import de.xbrowniecodez.jbytemod.Main;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.Frame;

import static org.objectweb.asm.Opcodes.*;

public class MethodUtils {
    public static MethodNode copy(MethodNode mn){
        MethodNode toReturn = new MethodNode();
        toReturn.access = mn.access;
        toReturn.maxLocals = mn.maxLocals;
        toReturn.maxStack = mn.maxStack;
        toReturn.desc = mn.desc;
        toReturn.instructions = mn.instructions;
        toReturn.name = mn.name;
        toReturn.exceptions = mn.exceptions;
        toReturn.tryCatchBlocks = mn.tryCatchBlocks;
        toReturn.invisibleAnnotations = mn.invisibleAnnotations;
        toReturn.localVariables = mn.localVariables;
        toReturn.signature = mn.signature;
        toReturn.invisibleLocalVariableAnnotations = mn.invisibleLocalVariableAnnotations;
        toReturn.invisibleParameterAnnotations = mn.invisibleParameterAnnotations;
        toReturn.invisibleTypeAnnotations = mn.invisibleTypeAnnotations;
        return toReturn;
    }

    public static boolean equalName(ClassNode classNode, String name){
        for (MethodNode method : classNode.methods) {
            if(method.name.equals(name)) return true;
        }

        return false;
    }

    public static void clear(MethodNode mn) {
        mn.instructions.clear();
        if(mn.name.equals("init")){
            mn.instructions.add(new VarInsnNode(ALOAD, 0));
            mn.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));
        }
        mn.instructions.add(generateReturn(mn.desc));
        mn.tryCatchBlocks.clear();
        mn.localVariables.clear();
        mn.exceptions.clear();
        mn.maxStack = 1;
        mn.maxLocals = 1;
    }

    public static InsnList generateReturn(String desc) {
        InsnList a = new InsnList();
        String after = desc.split("\\)")[1];
        a.add(new LabelNode());
        if (after.startsWith("[") || after.endsWith(";")) {
            a.add(new InsnNode(ACONST_NULL));
            a.add(new InsnNode(ARETURN));
        } else {
            switch (desc.toCharArray()[desc.length() - 1]) {
                case 'V':
                    a.add(new InsnNode(RETURN));
                    break;
                case 'D':
                    a.add(new InsnNode(DCONST_0));
                    a.add(new InsnNode(DRETURN));
                    break;
                case 'F':
                    a.add(new InsnNode(FCONST_0));
                    a.add(new InsnNode(FRETURN));
                    break;
                case 'J':
                    a.add(new InsnNode(LCONST_0));
                    a.add(new InsnNode(LRETURN));
                    break;
                default:
                    a.add(new InsnNode(ICONST_0));
                    a.add(new InsnNode(IRETURN));
                    break;
            }
        }
        return a;
    }

    public static void removeLines(MethodNode mn) {
        int i = 0;
        for (AbstractInsnNode ain : mn.instructions.toArray()) {
            if (ain instanceof LineNumberNode) {
                mn.instructions.remove(ain);
                i++;
            }
        }
         Main.INSTANCE.getLogger().log("Removed " + i + " nodes!");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void removeDeadCode(ClassNode cn, MethodNode mn) {
        Analyzer analyzer = new Analyzer(new BasicInterpreter());
        try {
            analyzer.analyze(cn.name, mn);
        } catch (AnalyzerException e) {
            ErrorDisplay.error("Could not analyze the code: " + e.getMessage());
            return;
        }
        Frame[] frames = analyzer.getFrames();
        AbstractInsnNode[] insns = mn.instructions.toArray();
        for (int i = 0; i < frames.length; i++) {
            AbstractInsnNode insn = insns[i];
            if (frames[i] == null && insn.getType() != AbstractInsnNode.LABEL) {
                mn.instructions.remove(insn);
                insns[i] = null;
            }
        }
    }
}
