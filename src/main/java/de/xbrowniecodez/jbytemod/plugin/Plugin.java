package de.xbrowniecodez.jbytemod.plugin;

import de.xbrowniecodez.jbytemod.Main;
import lombok.Getter;
import lombok.Setter;
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
        return Main.INSTANCE.getJByteMod().getJarArchive().getClasses();
    }

    protected final void updateTree() {
        Main.INSTANCE.getJByteMod().refreshTree();
    }

    protected final JMenuBar getMenu() {
        return Main.INSTANCE.getJByteMod().getMyMenuBar();
    }

    protected final JTree getTree() {
        return Main.INSTANCE.getJByteMod().getJarTree();
    }

    protected final ClassNode getSelectedNode() {
        return Main.INSTANCE.getJByteMod().getCurrentNode();
    }

    protected final MethodNode getSelectedMethod() {
        return Main.INSTANCE.getJByteMod().getCurrentMethod();
    }

}
