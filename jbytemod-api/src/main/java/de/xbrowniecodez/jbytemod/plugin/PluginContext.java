package de.xbrowniecodez.jbytemod.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.JMenuBar;
import javax.swing.JTree;
import java.util.Map;

public interface PluginContext {
    Map<String, ClassNode> getCurrentFile();

    void updateTree();

    JMenuBar getMenu();

    JTree getTree();

    ClassNode getSelectedNode();

    MethodNode getSelectedMethod();
}
