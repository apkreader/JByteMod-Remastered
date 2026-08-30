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
import java.util.function.IntConsumer;

public final class RemoteJarArchive extends JarArchive implements Closeable {
    private final RemoteAgentConnection connection;

    public RemoteJarArchive(RemoteAgentConnection connection) throws Exception {
        this(connection, progress -> {
        });
    }

    public RemoteJarArchive(RemoteAgentConnection connection, IntConsumer progress) throws Exception {
        super(new HashMap<>(), new HashMap<>());
        this.connection = connection;
        refresh(progress);
    }

    public synchronized void refresh() throws Exception {
        refresh(progress -> {
        });
    }

    public synchronized void refresh(IntConsumer progress) throws Exception {
        Map<String, byte[]> loaded = connection.loadClasses(value -> progress.accept(value * 90 / 100));
        Map<String, ClassNode> parsedClasses = new LinkedHashMap<>();
        Map<String, byte[]> originalBytes = new LinkedHashMap<>();
        int skipped = 0;
        int writerFlags = Main.INSTANCE.getJByteMod().getOptions().get("compute_maxs").getBoolean()
                ? ClassWriter.COMPUTE_MAXS
                : 0;
        int processed = 0;
        for (Map.Entry<String, byte[]> entry : loaded.entrySet()) {
            try {
                ClassNode classNode = BytecodeUtils.getClassNodeFromBytes(entry.getValue());
                parsedClasses.put(entry.getKey(), classNode);
                originalBytes.put(entry.getKey(), BytecodeUtils.getClassNodeBytes(classNode, writerFlags));
            } catch (Throwable ignored) {
                skipped++;
            }
            processed++;
            if (processed % 25 == 0 || processed == loaded.size()) {
                progress.accept(loaded.isEmpty() ? 100 : 90 + processed * 10 / loaded.size());
            }
        }
        if (loaded.isEmpty()) progress.accept(100);
        classes = parsedClasses;
        output = originalBytes;
        Main.INSTANCE.getLogger().log("Loaded " + classes.size() + " classes from attached process"
                + (skipped == 0 ? "" : " (skipped " + skipped + ")"));
    }

    public int redefine(Map<String, byte[]> classes) throws Exception {
        return connection.redefineClasses(classes);
    }

    public void terminate() throws Exception {
        connection.terminate();
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}
