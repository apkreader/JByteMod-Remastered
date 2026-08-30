package de.xbrowniecodez.jbytemod.utils.task;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.JarArchive;
import me.grax.jbytemod.ui.PageEndPanel;
import me.grax.jbytemod.utils.ErrorDisplay;
import de.xbrowniecodez.jbytemod.utils.attach.RemoteJarArchive;
import de.xbrowniecodez.jbytemod.utils.attach.RuntimeJarArchive;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import javax.swing.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.Map.Entry;

public class RetransformTask extends SwingWorker<Void, Integer> {

    private Instrumentation ins;
    private PageEndPanel jpb;
    private JarArchive file;

    public RetransformTask(JByteMod jbm, Instrumentation ins, JarArchive file) {
        this.ins = ins;
        this.file = file;
        this.jpb = jbm.getPageEndPanel();
    }

    @Override
    protected Void doInBackground() throws Exception {
        publish(0);
        try {
            Map<String, ClassNode> classes = file.getClasses();
            Map<String, byte[]> original = file.getOutput();
            Map<String, byte[]> newOriginal = new HashMap<>();

            ArrayList<ClassDefinition> definitions = new ArrayList<>();
            double size = classes.size();
            if (size == 0) {
                publish(100);
                return null;
            }
            int i = 0;
            int writerFlags = Main.INSTANCE.getJByteMod().getOptions().get("compute_maxs").getBoolean()
                    ? ClassWriter.COMPUTE_MAXS
                    : 0;
            for (Entry<String, ClassNode> e : classes.entrySet()) {
                publish((int) ((i / size) * 80d));
                byte[] originalBytes = original.get(e.getKey());
                byte[] bytes = BytecodeUtils.getClassNodeBytes(e.getValue(), writerFlags);
                //probably not the best solution but whatever
                if (!Arrays.equals(bytes, originalBytes)) {
                    if (!(file instanceof RemoteJarArchive)) {
                        Class<?> runtimeClass = file instanceof RuntimeJarArchive runtimeArchive
                                ? runtimeArchive.getRuntimeClass(e.getKey())
                                : ClassLoader.getSystemClassLoader().loadClass(e.getKey().replace('/', '.'));
                        if (runtimeClass == null) {
                            throw new ClassNotFoundException(e.getKey().replace('/', '.'));
                        }
                        definitions.add(new ClassDefinition(runtimeClass, bytes));
                    }
                    newOriginal.put(e.getKey(), bytes);
                }
                i++;
            }
            if (!newOriginal.isEmpty()) {
                publish(80);
                if (file instanceof RemoteJarArchive remoteArchive) {
                    remoteArchive.redefine(newOriginal);
                } else {
                    ins.redefineClasses(definitions.toArray(new ClassDefinition[0]));
                }
                 Main.INSTANCE.getLogger().log("Successfully retransformed " + newOriginal.size() + " classes");
                original.putAll(newOriginal);
            }
        } catch (VerifyError v) {
            JOptionPane.showMessageDialog(null, Main.INSTANCE.getJByteMod().getLanguageRes().getResource("verify_error"));
        } catch (Throwable t) {
            new ErrorDisplay(t);
            t.printStackTrace();
        }
        publish(100);
        return null;
    }

    @Override
    protected void process(List<Integer> chunks) {
        int i = chunks.get(chunks.size() - 1);
        jpb.setValue(i);
        super.process(chunks);
    }
}
