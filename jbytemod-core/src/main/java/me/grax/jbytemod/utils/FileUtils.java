package me.grax.jbytemod.utils;

import org.objectweb.asm.tree.ClassNode;

import java.io.File;

public class FileUtils {
    public static boolean exists(File f) {
        return f.exists() && !f.isDirectory();
    }

    public static boolean isType(File f, String... types) {
        for (String type : types) {
            if (f.getName().endsWith(type)) {
                return true;
            }
        }
        return false;
    }

    public static int isBadClass(ClassNode classNode){
        int toReturn = 0;

        if(classNode.methods.isEmpty()) toReturn =+ 10;
        if(classNode.fields.isEmpty() || classNode.fields.size() == 32) toReturn =+ 10;
        if(classNode.fields.size() > 500) toReturn += 50;
        if(classNode.fields.size() == 32) toReturn += 30;
        if(classNode.version > 55) toReturn += 10;
        if(classNode.access == 0 && classNode.methods.size() == 0) toReturn += 50;
        if(classNode.access == 0) toReturn += 20;
        if(classNode.name == null) toReturn = 100;
        if(classNode.version == 49) toReturn += 20;
        if(classNode.methods.size() == 2
                && classNode.methods.get(0).name.equals("<init>")
                && classNode.methods.get(1).access == 10
                && classNode.version == 49)
            toReturn += 100;
        if(classNode.access == 33 && classNode.methods.size() == 1 && classNode.fields.isEmpty()) toReturn += 100;

        if(toReturn > 100) toReturn = 100;
        return toReturn;
    }
}
