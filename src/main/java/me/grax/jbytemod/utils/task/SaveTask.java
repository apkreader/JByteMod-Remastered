package me.grax.jbytemod.utils.task;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.JarArchive;
import me.grax.jbytemod.ui.PageEndPanel;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.objectweb.asm.tree.ClassNode;

import de.xbrowniecodez.jbytemod.asm.CustomClassWriter;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class SaveTask extends SwingWorker<Void, Integer> {

    private final File output;
    private final PageEndPanel jpb;
    private final JarArchive file;

    public SaveTask(JByteMod jbm, File output, JarArchive file) {
        this.output = output;
        this.file = file;
        this.jpb = jbm.getPageEndPanel();
    }

    @Override
    protected Void doInBackground() throws Exception {
        synchronized (this.file) {
            try {
                Map<String, ClassNode> classes = this.file.getClasses();
                Map<String, byte[]> outputBytes = this.file.getOutput();
                int flags = Main.getInstance().getJByteMod().getOptions().get("compute_maxs").getBoolean() ? 1 : 0;
                 Main.getInstance().getLogger().log("Writing..");
                if (this.file.isSingleEntry()) {
                    ClassNode node = classes.values().iterator().next();
                    CustomClassWriter writer = new CustomClassWriter(flags);
                    node.accept(writer);
                    publish(50);
                    Main.getInstance().getLogger().log("Saving..");
                    Files.write(new File(this.output.toString().replace(".jar", ".class")).toPath(), writer.toByteArray());
                    publish(100);
                    Main.getInstance().getLogger().log("Saving successful!");
                    return null;
                }

                publish(0);
                double size = classes.keySet().size();
                double i = 0;
                for (String s : classes.keySet()) {
                    try{
                        ClassNode node = classes.get(s);
                        CustomClassWriter writer = new CustomClassWriter(flags);
                        node.accept(writer);
                        outputBytes.put(s + ".class", writer.toByteArray());
                        publish((int) ((i++ / size) * 50d));
                    }catch(StringIndexOutOfBoundsException exception) {
                         Main.getInstance().getLogger().println("Failed to save " + classes.get(s).name);
                    }
                }
                publish(50);
                 Main.getInstance().getLogger().log("Saving..");
                this.saveAsJarNew(outputBytes, output.getAbsolutePath());
                 Main.getInstance().getLogger().log("Saving successful!");
            } catch (Exception e) {
                e.printStackTrace();
                 Main.getInstance().getLogger().log("Saving failed!");
            }
            publish(100);
            return null;
        }
    }

    public void saveAsJarNew(Map<String, byte[]> outBytes, String fileName) {
        try {
            ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(Paths.get(fileName)));
            out.setEncoding("UTF-8");
            for (String entry : outBytes.keySet()) {
                out.putNextEntry(new ZipEntry(entry));
                if (!entry.endsWith("/") || !entry.endsWith("\\"))
                    out.write(outBytes.get(entry));
                out.closeEntry();
            }
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void process(List<Integer> chunks) {
        int i = chunks.get(chunks.size() - 1);
        jpb.setValue(i);
        super.process(chunks);
    }

}