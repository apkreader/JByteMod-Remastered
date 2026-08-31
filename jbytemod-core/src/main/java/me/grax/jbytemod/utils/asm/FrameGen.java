package me.grax.jbytemod.utils.asm;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import de.xbrowniecodez.jbytemod.JByteMod;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public final class FrameGen {
    private FrameGen() {
    }

    public static void regenerateFrames(JByteMod jbm, ClassNode cn) {
        ClassWriter cw = new LibClassWriter(ClassWriter.COMPUTE_FRAMES, jbm.getJarArchive().getClasses());
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

}
