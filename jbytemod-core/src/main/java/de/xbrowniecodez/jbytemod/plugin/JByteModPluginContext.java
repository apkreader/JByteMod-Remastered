package de.xbrowniecodez.jbytemod.plugin;

import de.xbrowniecodez.jbytemod.JByteMod;
import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.decompiler.ASMifierDecompiler;
import de.xbrowniecodez.jbytemod.decompiler.JDCoreDecompiler;
import de.xbrowniecodez.jbytemod.decompiler.VineflowerDecompiler;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import de.xbrowniecodez.jbytemod.utils.attach.RemoteJarArchive;
import de.xbrowniecodez.jbytemod.utils.attach.RuntimeJarArchive;
import me.grax.jbytemod.JarArchive;
import me.grax.jbytemod.decompiler.CFRDecompiler;
import me.grax.jbytemod.decompiler.Decompiler;
import me.grax.jbytemod.decompiler.KoffeeDecompiler;
import me.grax.jbytemod.decompiler.ProcyonDecompiler;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.JMenuBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class JByteModPluginContext implements PluginContext {
    private final JByteMod jByteMod;

    public JByteModPluginContext(JByteMod jByteMod) {
        this.jByteMod = Objects.requireNonNull(jByteMod, "jByteMod");
    }

    @Override
    public String getApplicationVersion() {
        return jByteMod.getVersion().toString();
    }

    @Override
    public ArchiveInfo getArchiveInfo() {
        JarArchive archive = jByteMod.getJarArchive();
        if (archive == null || archive.getClasses() == null) {
            return new ArchiveInfo(ArchiveType.NONE, 0, jByteMod.getLastEditFile());
        }

        ArchiveType type = archive instanceof RemoteJarArchive ? ArchiveType.REMOTE_JVM
                : archive instanceof RuntimeJarArchive ? ArchiveType.CURRENT_JVM
                : archive.isSingleEntry() ? ArchiveType.CLASS : ArchiveType.ARCHIVE;
        int resourceCount = archive.getOutput() == null ? 0 : archive.getOutput().size();
        return new ArchiveInfo(type, resourceCount, jByteMod.getLastEditFile());
    }

    @Override
    public Map<String, ClassNode> getCurrentFile() {
        JarArchive archive = jByteMod.getJarArchive();
        return archive == null || archive.getClasses() == null
                ? Collections.emptyMap() : archive.getClasses();
    }

    @Override
    public byte[] getClassBytes(ClassNode classNode) {
        return BytecodeUtils.getClassNodeBytes(classNode);
    }

    @Override
    public ClassNode readClass(byte[] bytes) {
        return BytecodeUtils.getClassNodeFromBytes(bytes);
    }

    @Override
    public List<String> getDecompilerIds() {
        return List.of("cfr", "procyon", "vineflower", "jd-core", "koffee", "asmifier");
    }

    @Override
    public String decompile(ClassNode classNode, MethodNode method, String decompilerId) {
        String id = Objects.requireNonNull(decompilerId, "decompilerId").trim().toLowerCase(Locale.ROOT);
        Decompiler decompiler = switch (id) {
            case "cfr" -> new CFRDecompiler(jByteMod, null);
            case "procyon" -> new ProcyonDecompiler(jByteMod, null);
            case "vineflower" -> new VineflowerDecompiler(jByteMod, null);
            case "jd-core", "jdcore" -> new JDCoreDecompiler(jByteMod, null);
            case "koffee" -> new KoffeeDecompiler(jByteMod, null);
            case "asmifier" -> new ASMifierDecompiler(jByteMod, null);
            default -> throw new IllegalArgumentException("Unknown decompiler: " + decompilerId);
        };
        decompiler.setNode(classNode, method);
        synchronized (Decompiler.class) {
            return decompiler.decompile(BytecodeUtils.getClassNodeBytes(classNode), method);
        }
    }

    @Override
    public void selectClass(ClassNode classNode) {
        runOnEdt(() -> jByteMod.treeSelection(classNode));
    }

    @Override
    public void selectMethod(ClassNode classNode, MethodNode method) {
        runOnEdt(() -> jByteMod.treeSelection(classNode, method));
    }

    @Override
    public void methodModified(ClassNode classNode, MethodNode method) {
        Decompiler.clearCache();
        if (jByteMod.getJarTree() == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (jByteMod.getCurrentNode() == classNode && jByteMod.getCurrentMethod() == method) {
                jByteMod.selectMethod(classNode, method);
            } else {
                jByteMod.getJarTree().repaint();
            }
        });
    }

    @Override
    public void replaceClass(ClassNode previous, ClassNode replacement) {
        Map<String, ClassNode> classes = getCurrentFile();
        if (classes.isEmpty() && getArchiveInfo().type() == ArchiveType.NONE) {
            throw new IllegalStateException("No archive is open in JByteMod");
        }

        classes.remove(previous.name);
        classes.put(replacement.name, replacement);
        Decompiler.clearCache();
        if (jByteMod.getJarTree() == null) {
            return;
        }

        boolean wasSelected = jByteMod.getCurrentNode() == previous;
        MethodNode selectedMethod = jByteMod.getCurrentMethod();
        MethodNode replacementMethod = selectedMethod == null ? null : replacement.methods.stream()
                .filter(method -> method.name.equals(selectedMethod.name) && method.desc.equals(selectedMethod.desc))
                .findFirst().orElse(null);
        SwingUtilities.invokeLater(() -> {
            jByteMod.refreshTree();
            if (!wasSelected) {
                return;
            }
            if (replacementMethod == null) {
                jByteMod.treeSelection(replacement);
            } else {
                jByteMod.treeSelection(replacement, replacementMethod);
            }
        });
    }

    @Override
    public void log(String message) {
        Main.INSTANCE.getLogger().log(message);
    }

    @Override
    public void logError(String message, Throwable error) {
        Main.INSTANCE.getLogger().err(message);
        if (error != null) {
            error.printStackTrace();
        }
    }

    @Override
    public void updateTree() {
        jByteMod.refreshTree();
    }

    @Override
    public JMenuBar getMenu() {
        return jByteMod.getMyMenuBar();
    }

    @Override
    public JTree getTree() {
        return jByteMod.getJarTree();
    }

    @Override
    public ClassNode getSelectedNode() {
        return jByteMod.getCurrentNode();
    }

    @Override
    public MethodNode getSelectedMethod() {
        return jByteMod.getCurrentMethod();
    }

    private static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while updating the JByteMod UI", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Could not update the JByteMod UI", exception.getCause());
        }
    }
}
