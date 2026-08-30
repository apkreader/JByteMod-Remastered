package de.xbrowniecodez.jbytemod.utils;

import de.xbrowniecodez.jbytemod.asm.CustomClassReader;
import de.xbrowniecodez.jbytemod.asm.CustomClassWriter;
import lombok.experimental.UtilityClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.Optional;

@UtilityClass
public class BytecodeUtils implements Opcodes {
    /**
     * @return The space left before the JVM maximum
     */
    public int getMethodSpaceLeft(MethodNode methodNode) {
        CodeSizeEvaluator cse = new CodeSizeEvaluator(null);
        methodNode.accept(cse);
        return (65534 - cse.getMaxSize());
    }

    public boolean isMethodSpaceLeft(MethodNode methodNode) {
        return getMethodSpaceLeft(methodNode) > 10000;
    }

    /**
     * @return If a class is an {@link Enum}
     */
    public boolean isEnum(ClassNode classNode) {
        return classNode.superName != null && classNode.superName.equals("java/lang/Enum");
    }

    /**
     * @return If a classes access is an Interface
     */
    public boolean isInterface(ClassNode classNode) {
        return Modifier.isInterface(classNode.access);
    }

    /**
     * @return If a methodNode matches the main method criteria
     */
    public boolean isMainMethod(MethodNode methodNode) {
        return methodNode.name.equals("main") && methodNode.desc.equals("([Ljava/lang/String;)V");
    }

    /**
     * @return If a methodNode matches the lambda method structure
     */
    public boolean isLambdaMethod(MethodNode methodNode) {
        return methodNode.name.startsWith("lambda$");
    }

    /**
     * @return If a methodNode can be identified as an initializer
     */
    public boolean isInitializer(final MethodNode methodNode) {
        return methodNode.name.contains("<") || methodNode.name.contains(">");
    }

    /**
     * @return If a methodNode is the main methods
     */
    public boolean isEntryPoint(MethodNode methodNode) {
        return methodNode.name.equals("main") && methodNode.desc.equals("([Ljava/lang/String;)V")
                && Modifier.isStatic(methodNode.access) && Modifier.isPublic(methodNode.access);
    }

    public boolean isInitializer(final String string) {
        return string.startsWith("<") || string.endsWith(">");
    }

    /**
     * @return If an {@link AbstractInsnNode} is an instance of a AbstractInsnNode
     */
    public static boolean isIntInsn(AbstractInsnNode insn) {
        if (insn == null) {
            return false;
        }

        int opcode = insn.getOpcode();

        return ((opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5)
                || opcode == Opcodes.BIPUSH
                || opcode == Opcodes.SIPUSH
                || (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Integer));
    }

    public static MethodNode getOrCreateClinit(final ClassNode classNode) {
        final Optional<MethodNode> optional = classNode.methods
                .stream()
                .filter(methodNode -> methodNode.name.equals("<clinit>")).findFirst();

        if (optional.isPresent()) {
            return optional.get();
        }

        final MethodNode methodNode = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        methodNode.visitCode();
        methodNode.visitInsn(Opcodes.RETURN);
        methodNode.visitEnd();
        classNode.methods.add(methodNode);
        return methodNode;
    }

    public static MethodNode getOrCreateInit(final ClassNode classNode) {
        final Optional<MethodNode> optional = classNode.methods
                .stream()
                .filter(methodNode -> methodNode.name.equals("<init>")).findFirst();

        if (optional.isPresent()) {
            return optional.get();
        }

        final MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        methodNode.visitCode();
        methodNode.visitInsn(Opcodes.RETURN);
        methodNode.visitEnd();
        classNode.methods.add(methodNode);
        return methodNode;
    }


    public static AbstractInsnNode toInsnNode(final int value) {
        if (value >= -1 && value <= 5) {
            return new InsnNode(value + 0x3);
        } else if (value > Byte.MIN_VALUE && value < Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, value);
        } else if (value > Short.MIN_VALUE && value < Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, value);
        }
        return new LdcInsnNode(value);
    }

    public static boolean isLongInsn(AbstractInsnNode insn) {
        if (insn == null)
            return false;

        int opcode = insn.getOpcode();
        return (opcode == Opcodes.LCONST_0
                || opcode == Opcodes.LCONST_1
                || (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Long));
    }

    public static boolean isFloatInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return (opcode >= Opcodes.FCONST_0 && opcode <= Opcodes.FCONST_2)
                || (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Float);
    }

    public static boolean isDoubleInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return (opcode >= Opcodes.DCONST_0 && opcode <= Opcodes.DCONST_1)
                || (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Double);
    }

    public static AbstractInsnNode getNumberInsn(int number) {
        if (number >= -1 && number <= 5) {
            return new InsnNode(number + 3);
        } else if (number >= -128 && number <= 127) {
            return new IntInsnNode(Opcodes.BIPUSH, number);
        } else if (number >= -32768 && number <= 32767) {
            return new IntInsnNode(Opcodes.SIPUSH, number);
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static AbstractInsnNode getNumberInsn(long number) {
        if (number >= 0 && number <= 1) {
            return new InsnNode((int) (number + 9));
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static AbstractInsnNode getLongInsn(long number) {
        return new LdcInsnNode(number);
    }

    public static AbstractInsnNode getNumberInsn(float number) {
        if (number >= 0 && number <= 2) {
            return new InsnNode((int) (number + 11));
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static AbstractInsnNode getNumberInsn(double number) {
        if (number >= 0 && number <= 1) {
            return new InsnNode((int) (number + 14));
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static int getIntegerFromInsn(AbstractInsnNode insn){
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
            return opcode - 3;
        } else if (insn instanceof IntInsnNode
                && insn.getOpcode() != Opcodes.NEWARRAY) {
            return ((IntInsnNode) insn).operand;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Integer) {
            return (Integer) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static long getLongFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.LCONST_0 && opcode <= Opcodes.LCONST_1) {
            return opcode - 9;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Long) {
            return (Long) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static float getFloatFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.FCONST_0 && opcode <= Opcodes.FCONST_2) {
            return opcode - 11;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Float) {
            return (Float) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static double getDoubleFromInsn(AbstractInsnNode insn)  {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.DCONST_0 && opcode <= Opcodes.DCONST_1) {
            return opcode - 14;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Double) {
            return (Double) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    /**
     * Returns the previous {@link AbstractInsnNode}.
     */
    public static AbstractInsnNode getPreviousInsn(AbstractInsnNode start, int amount) {
        AbstractInsnNode result = getPreviousInsn(start);
        for (int i = amount - 1; result != null && i > 0; i--) {
            result = getPreviousInsn(result);
        }
        return result;
    }

    /**
     * Returns the previous {@link AbstractInsnNode}.
     */
    public static AbstractInsnNode getPreviousInsn(AbstractInsnNode insn) {
        AbstractInsnNode previous = insn.getPrevious();
        while (previous != null && !isRealInsn(previous)) {
            previous = previous.getPrevious();
        }
        return previous;
    }

    /**
     * Returns the next {@link AbstractInsnNode}.
     */
    public static AbstractInsnNode getNextInsn(AbstractInsnNode start, int amount) {
        AbstractInsnNode result = getNextInsn(start);
        for (int i = amount - 1; result != null && i > 0; i--) {
            result = getNextInsn(result);
        }
        return result;
    }

    /**
     * Returns the next {@link AbstractInsnNode}.
     */
    public static AbstractInsnNode getNextInsn(AbstractInsnNode insn) {
        AbstractInsnNode next = insn.getNext();
        while (next != null && !isRealInsn(next)) {
            next = next.getNext();
        }
        return next;
    }

    /**
     * Returns {@code true} if the given {@link AbstractInsnNode} is an actual Java instruction.
     */
    public static boolean isRealInsn(AbstractInsnNode node) {
        return node != null && node.getOpcode() != -1;
    }

    /**
     * Returns access modifier without private or protected so that class
     * renaming works properly.
     *
     * @param access input access as {@link Integer}.
     * @return new {@link Integer} without restrictive flags.
     */
    public static int makePublic(int access) {
        int a = access;
        if ((a & Opcodes.ACC_PRIVATE) != 0) {
            a ^= Opcodes.ACC_PRIVATE;
        }
        if ((a & Opcodes.ACC_PROTECTED) != 0) {
            a ^= Opcodes.ACC_PROTECTED;
        }
        if ((a & Opcodes.ACC_PUBLIC) == 0) {
            a |= Opcodes.ACC_PUBLIC;
        }
        return a;
    }

    /**
     * Computes and returns the maximum number of local variables used in the given method.
     *
     * @param method a method.
     * @return the maximum number of local variables used in the given method.
     */
    public static int computeMaxLocals(final MethodNode method) {
        int maxLocals = Type.getArgumentsAndReturnSizes(method.desc) >> 2;
        if ((method.access & Opcodes.ACC_STATIC) != 0) {
            maxLocals -= 1;
        }
        for (AbstractInsnNode insnNode : method.instructions) {
            if (insnNode instanceof VarInsnNode) {
                int local = ((VarInsnNode) insnNode).var;
                int size =
                        (insnNode.getOpcode() == Opcodes.LLOAD
                                || insnNode.getOpcode() == Opcodes.DLOAD
                                || insnNode.getOpcode() == Opcodes.LSTORE
                                || insnNode.getOpcode() == Opcodes.DSTORE)
                                ? 2
                                : 1;
                maxLocals = Math.max(maxLocals, local + size);
            } else if (insnNode instanceof IincInsnNode) {
                int local = ((IincInsnNode) insnNode).var;
                maxLocals = Math.max(maxLocals, local + 1);
            }
        }
        return maxLocals;
    }

    /**
     * Gets the ClassNode of the provided bytes
     *
     * @param bytes the bytes of a ClassNode.
     * @return classNode from the given bytes
     */
    public static ClassNode getClassNodeFromBytes(byte[] bytes) {
        if(!ClassUtils.isClassFileFormat(bytes))
            throw new RuntimeException("Provided bytes are not a class file!");
        CustomClassReader classReader = new CustomClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

    /**
     * Get the java ClassFile of the provided classNode
     *
     * @param classNode the classNode to read.
     * @return Class<?> of the provided classNode
     */
    public static Class<?> getClassFromNode(ClassNode classNode) {
        CustomClassWriter classWriter = new CustomClassWriter(0);
        classNode.accept(classWriter);
        return new CustomClassLoader(ClassLoader.getSystemClassLoader()).get(classNode.name, classWriter.toByteArray());
    }

    public static byte[] getClassNodeBytes(ClassNode classNode) {
        return getClassNodeBytes(classNode, 0);
    }

    public static byte[] getClassNodeBytes(ClassNode classNode, int flags) {
        CustomClassWriter classWriter = new CustomClassWriter(flags);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    public static class CustomClassLoader extends ClassLoader {
        public CustomClassLoader(ClassLoader classLoader) {
            super(classLoader);
        }
        public Class<?> get(String name, byte[] bytes) {
            Class<?> clazz = defineClass(name.replace("/", "."), bytes, 0 , bytes.length);
            resolveClass(clazz);
            return clazz;
        }
    }

    public ClassNode createClassNode(String className) {
        ClassNode classNode = new ClassNode();

        //set the classNodes basic information
        classNode.version = Opcodes.V1_8;
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.name = className;
        classNode.superName = "java/lang/Object";
        classNode.interfaces = null;

        // we create a default constructor so its a valid class
        MethodNode defaultConstructor = createDefaultConstructor();
        classNode.methods.add(defaultConstructor);


        return classNode;
    }

    private MethodNode createDefaultConstructor() {
        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        methodNode.visitVarInsn(Opcodes.ALOAD, 0);
        methodNode.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        methodNode.visitInsn(Opcodes.RETURN);
        methodNode.visitMaxs(1, 1);
        return methodNode;
    }

    public static int getLabelIndex(AbstractInsnNode ain) {
        int index = 0;
        for (AbstractInsnNode node = ain; node.getPrevious() != null; node = node.getPrevious()) {
            if (node instanceof LabelNode) {
                index++;
            }
        }
        return index;
    }


}
