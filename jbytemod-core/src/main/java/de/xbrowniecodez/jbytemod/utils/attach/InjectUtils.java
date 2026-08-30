package de.xbrowniecodez.jbytemod.utils.attach;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class InjectUtils {
    public static void createAgentJar(File destination) throws IOException {
        Set<String> entries = new HashSet<>();
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination))) {
            addManifest(output, entries);
            addClass(AgentBootstrap.class, output, entries);
            addClass(AgentServer.class, output, entries);
        }
    }

    private static void addClass(Class<?> type, ZipOutputStream output, Set<String> entries) throws IOException {
        String name = type.getName().replace('.', '/') + ".class";
        if (!entries.add(name)) return;

        try (InputStream input = type.getResourceAsStream("/" + name)) {
            if (input == null) throw new IOException("Agent class not found: " + type.getName());
            output.putNextEntry(new ZipEntry(name));
            input.transferTo(output);
            output.closeEntry();
        }

        for (Class<?> nested : type.getDeclaredClasses()) {
            addClass(nested, output, entries);
        }
    }

    private static void addManifest(ZipOutputStream output, Set<String> entries) throws IOException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Agent-Class", AgentBootstrap.class.getName());
        attributes.putValue("Can-Redefine-Classes", "true");
        attributes.putValue("Can-Retransform-Classes", "true");
        attributes.putValue("Can-Set-Native-Method-Prefix", "false");

        output.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        manifest.write(output);
        output.closeEntry();
        entries.add("META-INF/MANIFEST.MF");
    }

}
