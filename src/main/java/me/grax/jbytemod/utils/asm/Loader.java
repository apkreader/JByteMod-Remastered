package me.grax.jbytemod.utils.asm;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Loader {
    public static ClassNode classToNode(String type) throws IOException {
        return BytecodeUtils.getClassNodeFromBytes(classToBytes(type));
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = is.read(buffer)) > 0) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }
}
