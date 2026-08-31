package de.xbrowniecodez.jbytemod.utils;

import org.objectweb.asm.Opcodes;

public final class AccessUtils {
    private AccessUtils() {
    }

    private static boolean has(int access, int flag) {
        return (access & flag) != 0;
    }

    public static boolean isPublic(int access) {
        return has(access, Opcodes.ACC_PUBLIC);
    }

    public static boolean isProtected(int access) {
        return has(access, Opcodes.ACC_PROTECTED);
    }

    public static boolean isPrivate(int access) {
        return has(access, Opcodes.ACC_PRIVATE);
    }

    public static boolean isStatic(int access) {
        return has(access, Opcodes.ACC_STATIC);
    }

    public static boolean isNative(int access) {
        return has(access, Opcodes.ACC_NATIVE);
    }

    public static boolean isAbstract(int access) {
        return has(access, Opcodes.ACC_ABSTRACT);
    }

    public static boolean isFinal(int access) {
        return has(access, Opcodes.ACC_FINAL);
    }

    public static boolean isSynthetic(int access) {
        return has(access, Opcodes.ACC_SYNTHETIC);
    }

    public static boolean isInterface(int access) {
        return has(access, Opcodes.ACC_INTERFACE);
    }

    public static boolean isEnum(int access) {
        return has(access, Opcodes.ACC_ENUM);
    }
}
