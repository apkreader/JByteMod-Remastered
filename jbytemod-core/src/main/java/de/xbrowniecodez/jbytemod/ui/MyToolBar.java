package de.xbrowniecodez.jbytemod.ui;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import de.xbrowniecodez.jbytemod.utils.attach.RemoteJarArchive;
import me.grax.jbytemod.ui.JAccessHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
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
            this.add(makeNavigationButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("reload"), getIcon("refresh"), e -> {
                jbm.refreshAgentClasses();
            }));
            this.add(makeNavigationButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("apply"), getIcon("save"), e -> {
                jbm.applyChangesAgent();
            }));
            if (jbm.getJarArchive() instanceof RemoteJarArchive) {
                this.add(makeNavigationButton("Terminate connected JVM", createTerminateIcon(), e -> {
                    jbm.terminateAttachedJvm();
                }));
            }
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
        java.net.URL resource = getClass().getResource("/resources/toolbar/" + string + ".png");
        if (resource == null) {
            Main.INSTANCE.getLogger().warn("Missing toolbar icon: " + string);
            return new ImageIcon();
        }
        return new ImageIcon(resource);
    }

    private Icon createTerminateIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Path2D stop = new Path2D.Double();
                    stop.moveTo(x + 5, y + 1);
                    stop.lineTo(x + 11, y + 1);
                    stop.lineTo(x + 15, y + 5);
                    stop.lineTo(x + 15, y + 11);
                    stop.lineTo(x + 11, y + 15);
                    stop.lineTo(x + 5, y + 15);
                    stop.lineTo(x + 1, y + 11);
                    stop.lineTo(x + 1, y + 5);
                    stop.closePath();
                    g.setColor(new Color(210, 58, 58));
                    g.fill(stop);
                    g.setColor(Color.WHITE);
                    g.fillRect(x + 5, y + 5, 6, 6);
                } finally {
                    g.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    protected JButton makeNavigationButton(String action, Icon i, ActionListener a) {
        JButton button = new JButton(i);
        button.setToolTipText(action);
        button.addActionListener(a);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setRolloverEnabled(false);
        return button;
    }
}
