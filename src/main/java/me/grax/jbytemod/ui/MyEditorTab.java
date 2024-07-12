package me.grax.jbytemod.ui;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;

public class MyEditorTab extends JPanel {
    private final JPanel code;
    private final JPanel info;
    private final DecompilerTab decompiler;
    private final JPanel center;
    private boolean classSelected;

    public MyEditorTab(JByteMod jbm) {
        setLayout(new BorderLayout());
        this.center = new JPanel();
        center.setLayout(new GridLayout());
        JLabel label = new JLabel("JByte Mod");

        MyCodeEditor codeEditor = new MyCodeEditor(jbm, label);
        jbm.setCodeList(codeEditor.getEditor());
        this.code = withBorder(label, codeEditor);

        InfoPanel sp = new InfoPanel(jbm);
        jbm.setInfoPanel(sp);

        this.info = this.withBorder(new JLabel(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("settings")), sp);

        this.decompiler = new DecompilerTab(jbm);
        this.decompiler.setName("decompiler");

        center.add(code);

        JPanel selector = getSelectionPanel(jbm);
        this.add(center, BorderLayout.CENTER);
        this.add(selector, BorderLayout.PAGE_END);
    }

    private @NotNull JPanel getSelectionPanel(JByteMod jbm) {
        JPanel selector = new JPanel();
        JButton codeBtn = new JButton("Code");
        codeBtn.setSelected(true);
        codeBtn.addActionListener(e -> showPanel(code));
        JButton infoBtn = new JButton("Info");
        infoBtn.addActionListener(e -> showPanel(info));
        JButton decompilerBtn = new JButton("Decompiler");
        decompilerBtn.addActionListener(e -> {
            showPanel(decompiler);
            decompiler.decompile(jbm.getCurrentNode(), jbm.getCurrentMethod(), false);
        });

        selector.add(codeBtn);
        selector.add(infoBtn);
        selector.add(decompilerBtn);
        selector.setLayout(new FlowLayout(FlowLayout.LEFT));
        return selector;
    }

    private void showPanel(Component panel) {
        if (center.getComponent(0) != panel) {
            center.removeAll();
            center.add(panel);
            center.revalidate();
            repaint();
        }
    }

    private JPanel withBorder(JLabel label, Component c) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 0));
        JPanel lpad = new JPanel();
        lpad.setBorder(new EmptyBorder(1, 5, 0, 5));
        lpad.setLayout(new GridLayout());
        lpad.add(label);
        panel.add(lpad, BorderLayout.NORTH);
        JScrollPane scp = new JScrollPane(c);
        scp.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scp, BorderLayout.CENTER);
        return panel;
    }

    public void selectClass(ClassNode cn) {
        Main.INSTANCE.getDiscord().updatePresence("Working on " + Main.INSTANCE.getJByteMod().getLastEditFile(), "Editing " + cn.name);

        String selectedComponentName = center.getComponent(0).getName();
        if (Objects.nonNull(selectedComponentName)) {
            if (selectedComponentName.equals("decompiler"))
                decompiler.decompile(cn, null, false);
        }
    }

    public void selectMethod(ClassNode cn, MethodNode mn) {
        Main.INSTANCE.getDiscord().updatePresence("Working on " + Main.INSTANCE.getJByteMod().getLastEditFile(), "Editing " + cn.name + "." + mn.name);

        String selectedComponentName = center.getComponent(0).getName();
        if (Objects.nonNull(selectedComponentName)) {
            if (selectedComponentName.equals("decompiler"))
                decompiler.decompile(cn, mn, false);
        }
    }
}
