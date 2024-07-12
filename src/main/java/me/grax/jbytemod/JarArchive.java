package me.grax.jbytemod;

import de.xbrowniecodez.jbytemod.JByteMod;
import lombok.Getter;
import lombok.Setter;
import me.grax.jbytemod.utils.ErrorDisplay;
import me.grax.jbytemod.utils.task.LoadTask;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class JarArchive {
    protected Map<String, ClassNode> classes;
    protected Map<String, byte[]> output;
    protected byte[] jarManifest;
    private boolean singleEntry;

    public JarArchive(ClassNode cn) {
        this.singleEntry = true;
        this.classes = new HashMap<>();
        this.classes.put(cn.name, cn);
    }

    public JarArchive(JByteMod jbm, File input) {
        try {
            new LoadTask(jbm, input, this).execute();
        } catch (Throwable t) {
            new ErrorDisplay(t);
        }
    }

    public JarArchive(Map<String, ClassNode> classes, Map<String, byte[]> output) {
        this.classes = classes;
        this.output = output;
    }
}
