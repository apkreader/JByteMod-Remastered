package de.xbrowniecodez.jbytemod.plugin;

import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.JarArchive;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.JMenuBar;
import javax.swing.JTree;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class JByteModPluginContext implements PluginContext {
    private final JByteMod jByteMod;

    public JByteModPluginContext(JByteMod jByteMod) {
        this.jByteMod = Objects.requireNonNull(jByteMod, "jByteMod");
    }

    public JByteMod getJByteMod() {
        return jByteMod;
    }

    @Override
    public Map<String, ClassNode> getCurrentFile() {
        JarArchive archive = jByteMod.getJarArchive();
        return archive == null || archive.getClasses() == null
                ? Collections.emptyMap() : archive.getClasses();
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
}
