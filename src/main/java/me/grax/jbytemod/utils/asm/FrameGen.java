package me.grax.jbytemod.utils.asm;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.utils.ErrorDisplay;

import me.lpk.util.JarUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import javax.swing.*;
import java.io.IOException;
import java.util.Map;

public class FrameGen extends Thread {

    private static Map<String, ClassNode> libraries;

    public static void regenerateFrames(JByteMod jbm, ClassNode cn) {
        if (libraries == null && Main.INSTANCE.getJByteMod().getOptions().get("use_rt").getBoolean()) {
            if (JOptionPane.showConfirmDialog(null, Main.INSTANCE.getJByteMod().getLanguageRes().getResource("load_rt")) == JOptionPane.OK_OPTION) {
                try {
                    libraries = JarUtils.loadRT();
                } catch (IOException e) {
                    new ErrorDisplay(e);
                }
                if (libraries == null) {
                    return;
                }
            } else {
                return;
            }
        }
        ClassWriter cw = new LibClassWriter(ClassWriter.COMPUTE_FRAMES, jbm.getJarArchive().getClasses(), libraries);
        try {
            cn.accept(cw);
            ClassNode node2 = BytecodeUtils.getClassNodeFromBytes(cw.toByteArray());
            cn.methods.clear();
            cn.methods.addAll(node2.methods);
             Main.INSTANCE.getLogger().log("Successfully regenerated frames at class " + cn.name);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void run() {
        try {
            libraries = JarUtils.loadRT();
             Main.INSTANCE.getLogger().log("Successfully loaded RT.jar");
        } catch (IOException e) {
            new ErrorDisplay(e);
        }
    }
}
