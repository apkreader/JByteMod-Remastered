package de.xbrowniecodez.jbytemod.plugin;

import de.xbrowniecodez.jbytemod.JByteMod;
import lombok.Getter;
import lombok.Setter;
import me.grax.jbytemod.JarArchive;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import java.util.Collections;
import java.util.Map;

@Getter
@Setter
public abstract class Plugin {
    protected String name;
    protected String version;
    protected String author;
    private JByteMod jByteMod;

    public Plugin(String name, String version, String author) {
        this.name = name;
        this.version = version;
        this.author = author;
    }

    final void attach(JByteMod jByteMod) {
        this.jByteMod = jByteMod;
    }

    public void init() {
    }

    public void loadFile(Map<String, ClassNode> map) {
    }

    public boolean isClickable() {
        return false;
    }

    public void menuClick() {
    }

    public void shutdown() {
    }

    protected final Map<String, ClassNode> getCurrentFile() {
        JarArchive archive = jByteMod.getJarArchive();
        return archive == null || archive.getClasses() == null ? Collections.emptyMap() : archive.getClasses();
    }

    protected final void updateTree() {
        jByteMod.refreshTree();
    }

    protected final JMenuBar getMenu() {
        return jByteMod.getMyMenuBar();
    }

    protected final JTree getTree() {
        return jByteMod.getJarTree();
    }

    protected final ClassNode getSelectedNode() {
        return jByteMod.getCurrentNode();
    }

    protected final MethodNode getSelectedMethod() {
        return jByteMod.getCurrentMethod();
    }

}
