package de.xbrowniecodez.jbytemod.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.JMenuBar;
import javax.swing.JTree;
import java.util.List;
import java.util.Map;

public interface PluginContext {
    String getApplicationVersion();

    ArchiveInfo getArchiveInfo();

    Map<String, ClassNode> getCurrentFile();

    byte[] getClassBytes(ClassNode classNode);

    ClassNode readClass(byte[] bytes);

    List<String> getDecompilerIds();

    void openFile(String path) throws Exception;

    String saveFile(String path) throws Exception;

    List<JvmProcess> listJvmProcesses();

    void attachToJvm(String pid) throws Exception;

    void refreshAttachedJvm() throws Exception;

    int applyChangesToAttachedJvm() throws Exception;

    String decompile(ClassNode classNode, MethodNode method, String decompilerId);

    void selectClass(ClassNode classNode);

    void selectMethod(ClassNode classNode, MethodNode method);

    void methodModified(ClassNode classNode, MethodNode method);

    void replaceClass(ClassNode previous, ClassNode replacement);

    void log(String message);

    void logError(String message, Throwable error);

    void updateTree();

    JMenuBar getMenu();

    JTree getTree();

    ClassNode getSelectedNode();

    MethodNode getSelectedMethod();
}
