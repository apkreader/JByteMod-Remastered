package de.xbrowniecodez.jbytemod.utils.attach;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import me.grax.jbytemod.JarArchive;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RemoteJarArchive extends JarArchive implements Closeable {
    private final RemoteAgentConnection connection;

    public RemoteJarArchive(RemoteAgentConnection connection) throws Exception {
        super(new HashMap<>(), new HashMap<>());
        this.connection = connection;
        refresh();
    }

    public synchronized void refresh() throws Exception {
        Map<String, byte[]> loaded = connection.loadClasses();
        Map<String, ClassNode> parsedClasses = new LinkedHashMap<>();
        Map<String, byte[]> originalBytes = new LinkedHashMap<>();
        int skipped = 0;
        int writerFlags = Main.INSTANCE.getJByteMod().getOptions().get("compute_maxs").getBoolean()
                ? ClassWriter.COMPUTE_MAXS
                : 0;
        for (Map.Entry<String, byte[]> entry : loaded.entrySet()) {
            try {
                ClassNode classNode = BytecodeUtils.getClassNodeFromBytes(entry.getValue());
                parsedClasses.put(entry.getKey(), classNode);
                originalBytes.put(entry.getKey(), BytecodeUtils.getClassNodeBytes(classNode, writerFlags));
            } catch (Throwable ignored) {
                skipped++;
            }
        }
        classes = parsedClasses;
        output = originalBytes;
        Main.INSTANCE.getLogger().log("Loaded " + classes.size() + " classes from attached process"
                + (skipped == 0 ? "" : " (skipped " + skipped + ")"));
    }

    public int redefine(Map<String, byte[]> classes) throws Exception {
        return connection.redefineClasses(classes);
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}
