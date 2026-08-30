package de.xbrowniecodez.jbytemod.utils.asm;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Loader {
    public static ClassNode classToNode(String type) throws IOException {
        byte[] bytes = classToBytes(type);
        return bytes == null ? null : BytecodeUtils.getClassNodeFromBytes(bytes);
    }

    public static ClassNode classToNode(Class<?> type) throws IOException {
        byte[] bytes = classToBytes(type);
        return bytes == null ? null : BytecodeUtils.getClassNodeFromBytes(bytes);
    }

    public static byte[] classToBytes(String type) throws IOException {
        if (type == null) {
            return null;
        }
        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(type + ".class");
        if (is == null) {
            Main.INSTANCE.getLogger().err(type + " not in classpath");
            return null;
        }
        return readBytes(is);
    }

    public static byte[] classToBytes(Class<?> type) throws IOException {
        String resourceName = type.getName().replace('.', '/') + ".class";
        InputStream input = type.getResourceAsStream("/" + resourceName);
        if (input == null && type.getClassLoader() != null) {
            input = type.getClassLoader().getResourceAsStream(resourceName);
        }
        return input == null ? null : readBytes(input);
    }

    private static byte[] readBytes(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        try (input) {
            while ((n = input.read(buffer)) > 0) {
                baos.write(buffer, 0, n);
            }
        }
        return baos.toByteArray();
    }
}
