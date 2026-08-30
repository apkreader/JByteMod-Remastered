package de.xbrowniecodez.jbytemod.utils.attach;

import de.xbrowniecodez.jbytemod.JByteMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class InjectUtils {
    private static final byte[] BUFFER = new byte[4096 * 1024];

    public static void createAgentJar(File destination) throws IOException {
        Set<String> entries = new HashSet<>();
        Set<Path> sources = new HashSet<>();
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination))) {
            addManifest(output, entries);
            String[] classPath = System.getProperty("java.class.path", "")
                    .split(Pattern.quote(File.pathSeparator));
            for (String element : classPath) {
                if (element.isBlank()) continue;
                addClassPathElement(new File(element), output, entries, sources);
            }
        }

        String bootstrapClass = AgentBootstrap.class.getName().replace('.', '/') + ".class";
        String applicationClass = JByteMod.class.getName().replace('.', '/') + ".class";
        if (!entries.contains(bootstrapClass) || !entries.contains(applicationClass)) {
            throw new IOException("Agent classes were not found on the runtime classpath");
        }
    }

    private static void addClassPathElement(File source, ZipOutputStream output, Set<String> entries,
                                            Set<Path> sources) throws IOException {
        Path path = source.toPath().toAbsolutePath().normalize();
        if (!Files.exists(path) || !sources.add(path)) return;

        if (Files.isDirectory(path)) {
            addDirectory(path, output, entries);
        } else if (source.getName().toLowerCase().endsWith(".jar")) {
            addJar(source, output, entries, sources);
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

    private static void addDirectory(Path root, ZipOutputStream output, Set<String> entries) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                String name = root.relativize(file).toString().replace(File.separatorChar, '/');
                if (shouldSkip(name) || !entries.add(name)) continue;

                output.putNextEntry(new ZipEntry(name));
                Files.copy(file, output);
                output.closeEntry();
            }
        }
    }

    private static void addJar(File source, ZipOutputStream output, Set<String> entries, Set<Path> sources) throws IOException {
        try (JarFile jar = new JarFile(source)) {
            Enumeration<? extends ZipEntry> sourceEntries = jar.entries();
            while (sourceEntries.hasMoreElements()) {
                ZipEntry entry = sourceEntries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || shouldSkip(name) || !entries.add(name)) continue;

                output.putNextEntry(new ZipEntry(name));
                try (InputStream input = jar.getInputStream(entry)) {
                    copy(input, output);
                }
                output.closeEntry();
            }

            Manifest sourceManifest = jar.getManifest();
            if (sourceManifest == null) return;

            String manifestClassPath = sourceManifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (manifestClassPath == null) return;

            for (String element : manifestClassPath.split("\\s+")) {
                if (element.isBlank()) continue;
                try {
                    File dependency = new File(source.toURI().resolve(element));
                    addClassPathElement(dependency, output, entries, sources);
                } catch (IllegalArgumentException ignored) {
                    // Non-file classpath entries cannot be bundled into the local agent jar.
                }
            }
        }
    }

    private static boolean shouldSkip(String name) {
        String upperName = name.toUpperCase();
        return name.startsWith("/") || name.contains("../")
                || upperName.equals("META-INF/MANIFEST.MF")
                || upperName.equals("META-INF/INDEX.LIST")
                || upperName.endsWith(".SF")
                || upperName.endsWith(".RSA")
                || upperName.endsWith(".DSA")
                || name.equals("module-info.class")
                || name.endsWith("/module-info.class");
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(BUFFER)) != -1) {
            output.write(BUFFER, 0, bytesRead);
        }
    }
}
