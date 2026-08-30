package me.grax.jbytemod.utils.asm;

import org.objectweb.asm.tree.ClassNode;

import java.util.Map;

public class ParentUtils {
    private Map<String, ClassNode> classes;

    public ParentUtils(Map<String, ClassNode> classes) {
        this.classes = classes;
    }

    public boolean isAssignableFrom(ClassNode cn, ClassNode cn2) {
        if (cn2.name.equals(cn.name)) {
            return true;
        }
        for (String itfn : cn2.interfaces) {
            ClassNode itf = classes.get(itfn);
            if (itf == null)
                continue;
            if (isAssignableFrom(cn, itf)) {
                return true;
            }
        }
        if (cn2.superName != null) {
            ClassNode sn = classes.get(cn2.superName);
            if (sn != null)
                if (isAssignableFrom(cn, sn)) {
                    return true;
                }
        }
        return false;
    }
}
