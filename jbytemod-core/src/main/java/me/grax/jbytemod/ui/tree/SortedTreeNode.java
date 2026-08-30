package me.grax.jbytemod.ui.tree;

import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

@Getter
@Setter
public class SortedTreeNode extends DefaultMutableTreeNode {

    private ClassNode classNode;
    private MethodNode methodNode;
    private String className;

    public SortedTreeNode(ClassNode classNode, MethodNode methodNode) {
        this.classNode = classNode;
        this.methodNode = methodNode;
        setClassName();
    }

    public SortedTreeNode(ClassNode classNode) {
        this.classNode = classNode;
        setClassName();
    }

    public SortedTreeNode(Object userObject) {
        super(userObject);
    }

    private void setClassName() {
        String[] split = classNode.name.split("/");
        this.className = split[split.length - 1] + ".class";
    }

    @SuppressWarnings("unchecked")
    public void sort() {
        if (children != null) {
            ((Vector<DefaultMutableTreeNode>) (Vector<?>) children).sort(compare());
        }
    }

    private Comparator<DefaultMutableTreeNode> compare() {
        return (o1, o2) -> {
            boolean leaf1 = o1.toString().endsWith(".class");
            boolean leaf2 = o2.toString().endsWith(".class");

            if (leaf1 && !leaf2) {
                return 1;
            }
            if (!leaf1 && leaf2) {
                return -1;
            }
            return o1.toString().compareTo(o2.toString());
        };
    }

    @Override
    public String toString() {
        if (methodNode != null) {
            return methodNode.name;
        }
        if (classNode != null) {
            return className;
        }
        return userObject != null ? userObject.toString() : "";
    }
}
