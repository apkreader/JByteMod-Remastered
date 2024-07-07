package de.xbrowniecodez.jbytemod.plugin;

import lombok.Getter;
import lombok.Setter;
import me.grax.jbytemod.JByteMod;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import java.util.Map;
@Getter
@Setter
public abstract class Plugin {
    protected String name;
    protected String version;
    protected String author;

    public Plugin(String name, String version, String author) {
        this.name = name;
        this.version = version;
        this.author = author;
    }

    public abstract void init();

    public abstract void loadFile(Map<String, ClassNode> map);

    public abstract boolean isClickable();

    public abstract void menuClick();

    protected final Map<String, ClassNode> getCurrentFile() {
        return JByteMod.instance.getJarArchive().getClasses();
    }

    protected final void updateTree() {
        JByteMod.instance.refreshTree();
    }

    protected final JMenuBar getMenu() {
        return JByteMod.instance.getMyMenuBar();
    }

    protected final JTree getTree() {
        return JByteMod.instance.getJarTree();
    }

    protected final ClassNode getSelectedNode() {
        return JByteMod.instance.getCurrentNode();
    }

    protected final MethodNode getSelectedMethod() {
        return JByteMod.instance.getCurrentMethod();
    }

}
