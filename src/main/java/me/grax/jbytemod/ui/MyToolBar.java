package me.grax.jbytemod.ui;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class MyToolBar extends JToolBar {
    private MyMenuBar menubar;

    public MyToolBar(JByteMod jbm) {
        this.menubar = (MyMenuBar) jbm.getJMenuBar();
        this.setFloatable(false);
        if (!menubar.isAgent()) {
            this.add(makeNavigationButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("load"), getIcon("load"), e -> {
                menubar.openLoadDialogue();
            }));
            this.add(makeNavigationButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("save"), getIcon("save"), e -> {
                if (menubar.getLastFile() != null) {
                    jbm.saveFile(menubar.getLastFile());
                } else {
                    menubar.openSaveDialogue();
                }
            }));
        } else {
            this.add(makeNavigationButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("reload"), getIcon("reload"), e -> {
                jbm.refreshAgentClasses();
            }));
            this.add(makeNavigationButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("apply"), getIcon("save"), e -> {
                jbm.applyChangesAgent();
            }));
        }
        this.addSeparator();
        this.add(makeNavigationButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("search"), getIcon("search"), e -> {
            menubar.searchLDC();
        }));
        this.addSeparator();
        this.add(makeNavigationButton("Access Helper", getIcon("table"), e -> {
            new JAccessHelper().setVisible(true);
        }));
        this.add(makeNavigationButton("Attach to other process", getIcon("plug"), e -> {
            menubar.openProcessSelection();
        }));
    }

    private ImageIcon getIcon(String string) {
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/toolbar/" + string + ".png")));
    }

    protected JButton makeNavigationButton(String action, ImageIcon i, ActionListener a) {
        JButton button = new JButton(i);
        button.setToolTipText(action);
        button.addActionListener(a);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setRolloverEnabled(false);
        return button;
    }
}