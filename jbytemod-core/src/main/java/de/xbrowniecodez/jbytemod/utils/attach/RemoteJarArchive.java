package de.xbrowniecodez.jbytemod.utils.attach;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.plugin.JvmClassLoaderInfo;
import de.xbrowniecodez.jbytemod.plugin.JvmRuntimeInfo;
import de.xbrowniecodez.jbytemod.plugin.JvmThreadInfo;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import me.grax.jbytemod.JarArchive;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

public final class RemoteJarArchive extends JarArchive implements Closeable {
    private final RemoteAgentConnection connection;
    private final long processId;
    private boolean frozen;

    public RemoteJarArchive(RemoteAgentConnection connection, long processId) throws Exception {
        this(connection, processId, progress -> {
        });
    }

    public RemoteJarArchive(RemoteAgentConnection connection, long processId, IntConsumer progress) throws Exception {
        super(new HashMap<>(), new HashMap<>());
        this.connection = connection;
        this.processId = processId;
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

    public synchronized JvmRuntimeInfo getRuntimeInfo() throws Exception {
        requireRunning();
        return connection.getRuntimeInfo(frozen);
    }

    public synchronized List<JvmThreadInfo> getThreads(int maxStackDepth) throws Exception {
        requireRunning();
        return connection.getThreads(maxStackDepth);
    }

    public synchronized List<JvmClassLoaderInfo> getClassLoaders() throws Exception {
        requireRunning();
        return connection.getClassLoaders();
    }

    public synchronized Map<String, String> getSystemProperties() throws Exception {
        requireRunning();
        return connection.getSystemProperties();
    }

    public synchronized byte[] invokeAgentExtension(String extensionId, String entryClassName,
                                                     Map<String, byte[]> classFiles, byte[] request) throws Exception {
        requireRunning();
        return connection.invokeAgentExtension(extensionId, entryClassName, classFiles, request);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public long getProcessId() {
        return processId;
    }

    private void requireRunning() {
        if (frozen) throw new IllegalStateException("Resume the attached JVM before inspecting it");
    }

    public synchronized void terminate() throws Exception {
        if (frozen) setFrozen(false);
        connection.terminate();
    }

    public synchronized void setFrozen(boolean frozen) throws Exception {
        if (this.frozen == frozen) return;
        ProcessSuspender.setSuspended(processId, frozen);
        this.frozen = frozen;
    }

    @Override
    public synchronized void close() throws IOException {
        IOException failure = null;
        if (frozen) {
            try {
                ProcessSuspender.setSuspended(processId, false);
                frozen = false;
            } catch (IOException exception) {
                failure = exception;
            }
        }
        try {
            connection.close();
        } catch (IOException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        if (failure != null) throw failure;
    }
}
