package de.xbrowniecodez.jbytemod.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;

/**
 * @author Cg.
 */
public class ClassUtils {
    /**
     * Class file magic number: 0xCAFEBABE.
     */
    public static final byte[] MAGIC = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};

    /**
     * Returns {@code true} if the file extension of the given filename is a class file.
     */
    public static boolean isClassFileExt(String fileName) {
        return fileName.endsWith(".class") || fileName.endsWith(".class/");
    }

    /**
     * Returns {@code true} if the given value is a class file format.
     */
    public static boolean isClassFileFormat(byte[] classBytes) {
        return classBytes.length > 4 && Arrays.equals(Arrays.copyOf(classBytes, 4), ClassUtils.MAGIC);
    }

    /**
     * Returns the {@link ClassNode} parsed by the given class file.
     */
    public static ClassNode loadClass(byte[] data) {
        return loadClass(data, 0);
    }

    /**
     * Returns the {@link ClassNode} parsed by the given class file with specific parsing options.
     */
    public static ClassNode loadClass(byte[] data, int flags) {
        ClassReader cr = new ClassReader(data);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, flags);
        return classNode;
    }

    /**
     * Returns the bytes of the class file assembled by the given {@link ClassNode}.
     */
    public static byte[] toBytes(ClassNode classNode) {
        return toBytes(classNode, 0);
    }

    /**
     * Returns the bytes of the class file assembled by the given {@link ClassNode} with specific writer flags.
     */
    public static byte[] toBytes(ClassNode classNode, int flags) {
        ClassWriter writer = new ClassWriter(flags);
        classNode.accept(writer);
        return writer.toByteArray();
    }


    /**
     * Returns the simple name of the class (without the package) in the given class name.
     */
    public static String getSimpleName(String className) {
        return className.substring(className.replace(".", "/").lastIndexOf("/") + 1);
    }

    /**
     * Check if ClassNode is an Enum
     */
    public static boolean isEnum(ClassNode classNode) {
        return classNode.superName.equals("java/lang/Enum");
    }

    public static ClassNode clone(ClassNode classNode) {
        return loadClass(toBytes(classNode, 0), 0);
    }

    public static ClassNode clone(ClassNode classNode, int readFlags, int writeFlags) {
        return loadClass(toBytes(classNode, writeFlags), readFlags);
    }
}
