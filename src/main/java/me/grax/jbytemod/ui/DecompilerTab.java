package me.grax.jbytemod.ui;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.decompiler.ASMifierDecompiler;
import de.xbrowniecodez.jbytemod.decompiler.JDCoreDecompiler;
import de.xbrowniecodez.jbytemod.decompiler.VineflowerDecompiler;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.decompiler.*;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class DecompilerTab extends JPanel {
    private static File tempDir = new File(System.getProperty("java.io.tmpdir"));
    private static File userDir = new File(System.getProperty("user.dir"));
    protected Decompilers decompiler = Decompilers.CFR;
    private DecompilerPanel dp;
    private JLabel label;
    private JByteMod jbm;
    private JButton compile = new JButton("Compile");

    public DecompilerTab(JByteMod jbm) {
        this.jbm = jbm;
        this.dp = new DecompilerPanel();
        this.label = new JLabel(decompiler + " Decompiler");
        jbm.setDecompilerPanel(dp);
        this.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        topPanel.setBorder(new EmptyBorder(1, 5, 5, 1));

        JPanel labelPanel = new JPanel(new GridLayout());
        labelPanel.add(label);

        JPanel rightPanel = new JPanel(new GridLayout(1, 5));
        for (int i = 0; i < 3; i++)
            rightPanel.add(new JPanel());

        JComboBox<Decompilers> decompilerCombo = new JComboBox<>(Decompilers.values());
        decompilerCombo.addActionListener(e -> {
            DecompilerTab.this.decompiler = (Decompilers) decompilerCombo.getSelectedItem();
            label.setText(decompiler.getName() + " " + decompiler.getVersion());
            decompile(Decompiler.last, Decompiler.lastMn, true);
        });
        rightPanel.add(decompilerCombo);

        compile.setVisible(false);
        rightPanel.add(compile);

        JButton reload = new JButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("reload"));
        reload.addActionListener(e -> decompile(Decompiler.last, Decompiler.lastMn, true));
        rightPanel.add(reload);
        
        labelPanel.add(rightPanel);
        topPanel.add(labelPanel, BorderLayout.CENTER);
        this.add(topPanel, BorderLayout.NORTH);

        JScrollPane scp = new RTextScrollPane(dp);
        scp.getVerticalScrollBar().setUnitIncrement(16);
        this.add(scp, BorderLayout.CENTER);
    }

    public void decompile(ClassNode cn, MethodNode mn, boolean deleteCache) {
        if (cn == null) {
            return;
        }
        Decompiler d = null;
        compile.setVisible(false);
        dp.setEditable(false);

        switch (decompiler) {
            case PROCYON:
                d = new ProcyonDecompiler(jbm, dp);
                break;
            case VINEFLOWER:
                d = new VineflowerDecompiler(jbm, dp);
                break;
            case CFR:
                d = new CFRDecompiler(jbm, dp);
                break;
            case KOFFEE:
                d = new KoffeeDecompiler(jbm, dp);
                break;
            case JDCORE:
                d = new JDCoreDecompiler(jbm, dp);
                break;
            case ASMIFIER:
                d = new ASMifierDecompiler(jbm, dp);
                break;
        }
        d.setNode(cn, mn);
        if (deleteCache) {
            d.deleteCache();
        }
        d.start();
    }

    public void compile(ClassNode cn, MethodNode mn) {
        //TODO: Maybe java edited recompilation
    }
}
