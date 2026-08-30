package de.xbrowniecodez.jbytemod.ui;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import de.xbrowniecodez.jbytemod.utils.attach.RemoteJarArchive;
import me.grax.jbytemod.ui.JAccessHelper;
import me.grax.jbytemod.utils.ErrorDisplay;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;

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
            JButton reload = makeNavigationButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("reload"), getIcon("refresh"), e -> {
                jbm.refreshAgentClasses();
            });
            this.add(reload);
            JButton apply = makeNavigationButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("apply"), getIcon("save"), e -> {
                jbm.applyChangesAgent();
            });
            this.add(apply);
            if (jbm.getJarArchive() instanceof RemoteJarArchive) {
                JToggleButton freeze = makeNavigationToggleButton("Freeze connected JVM", createFreezeIcon());
                freeze.addActionListener(e -> setFrozen(jbm, freeze, reload, apply));
                this.add(freeze);
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

    private Icon createFreezeIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(64, 140, 225));
                    g.fillRoundRect(x + 3, y + 2, 4, 12, 2, 2);
                    g.fillRoundRect(x + 9, y + 2, 4, 12, 2, 2);
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

    private Icon createResumeIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Path2D play = new Path2D.Double();
                    play.moveTo(x + 4, y + 2);
                    play.lineTo(x + 14, y + 8);
                    play.lineTo(x + 4, y + 14);
                    play.closePath();
                    g.setColor(new Color(70, 180, 95));
                    g.fill(play);
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

    private JToggleButton makeNavigationToggleButton(String action, Icon icon) {
        JToggleButton button = new JToggleButton(icon);
        button.setToolTipText(action);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setRolloverEnabled(false);
        return button;
    }

    private void setFrozen(JByteMod jbm, JToggleButton button, JButton... targetActions) {
        if (!(jbm.getJarArchive() instanceof RemoteJarArchive archive)) {
            button.setSelected(false);
            return;
        }

        boolean frozen = button.isSelected();
        button.setEnabled(false);
        setEnabled(targetActions, false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                archive.setFrozen(frozen);
                return null;
            }

            @Override
            protected void done() {
                boolean succeeded = false;
                try {
                    get();
                    button.setToolTipText(frozen ? "Resume connected JVM" : "Freeze connected JVM");
                    button.setIcon(frozen ? createResumeIcon() : createFreezeIcon());
                    succeeded = true;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    button.setSelected(!frozen);
                } catch (ExecutionException exception) {
                    button.setSelected(!frozen);
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    new ErrorDisplay(cause);
                } finally {
                    button.setEnabled(true);
                    if (!frozen || !succeeded) setEnabled(targetActions, true);
                }
            }
        }.execute();
    }

    private void setEnabled(JButton[] buttons, boolean enabled) {
        for (JButton button : buttons) button.setEnabled(enabled);
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
