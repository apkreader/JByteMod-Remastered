package de.xbrowniecodez.jbytemod.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;

public abstract class Plugin {
    protected String name;
    protected String version;
    protected String author;
    private PluginContext context;

    public Plugin(String name, String version, String author) {
        this.name = name;
        this.version = version;
        this.author = author;
    }

    final void attach(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
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

    protected final PluginContext getContext() {
        if (context == null) {
            throw new IllegalStateException("Plugin is not attached to JByteMod");
        }
        return context;
    }

    protected final Map<String, ClassNode> getCurrentFile() {
        return getContext().getCurrentFile();
    }

    protected final void updateTree() {
        getContext().updateTree();
    }

    protected final JMenuBar getMenu() {
        return getContext().getMenu();
    }

    protected final JTree getTree() {
        return getContext().getTree();
    }

    protected final ClassNode getSelectedNode() {
        return getContext().getSelectedNode();
    }

    protected final MethodNode getSelectedMethod() {
        return getContext().getSelectedMethod();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

}
