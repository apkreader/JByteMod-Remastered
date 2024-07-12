package me.grax.jbytemod.ui;

import com.github.weisj.darklaf.ui.tabbedpane.DarkTabbedPaneBorder;
import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.ui.ifs.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import java.awt.*;

public class InfoPanel extends JPanel {

    private JDesktopPane deskPane;
    private JByteMod jbm;

    public InfoPanel(JByteMod jbm) {
        Color backgroundColor = Main.INSTANCE.getJByteMod().getOptions().get("use_dark_theme").getBoolean() ? new Color(33, 37, 43) : Color.WHITE;
        this.jbm = jbm;
        this.setLayout(new BorderLayout());
        deskPane = new JDesktopPane() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(backgroundColor);
                g.fillRect(0, 0, getWidth(), getHeight());
            }

        };
        deskPane.setBorder(new DarkTabbedPaneBorder());
        deskPane.setDesktopManager(new DeskMan());
        this.add(deskPane, BorderLayout.CENTER);
    }

    public void selectMethod(ClassNode cn, MethodNode mn) {
        for (Component c : deskPane.getComponents()) {
            if (c instanceof MyInternalFrame) {
                c.setVisible(false);
            }
        }
        deskPane.removeAll();
        deskPane.add(new TCBFrame(jbm.getTcbList()));
        deskPane.add(new LVPFrame(jbm.getLvpList()));
        deskPane.add(new MNSettings(cn, mn));

        this.repaint();
    }

    public void selectClass(ClassNode cn) {
        for (Component c : deskPane.getComponents()) {
            if (c instanceof MyInternalFrame) {
                c.setVisible(false);
            }
        }
        deskPane.removeAll();
        deskPane.add(new CNSettings(cn));
        this.repaint();
    }

    class DeskMan extends DefaultDesktopManager {

        @Override
        public void beginDraggingFrame(JComponent f) {
        }

        @Override
        public void beginResizingFrame(JComponent f, int direction) {
        }

        @Override
        public void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
            if (!(f instanceof JInternalFrame)) {
                return;
            }
            boolean didResize = (f.getWidth() != newWidth || f.getHeight() != newHeight);
            if (!inBounds((JInternalFrame) f, newX, newY, newWidth, newHeight)) {
                Container parent = f.getParent();
                Dimension parentSize = parent.getSize();
                int boundedX = (int) Math.min(Math.max(0, newX), parentSize.getWidth() - newWidth);
                int boundedY = (int) Math.min(Math.max(0, newY), parentSize.getHeight() - newHeight);
                f.setBounds(boundedX, boundedY, newWidth, newHeight);
            } else {
                f.setBounds(newX, newY, newWidth, newHeight);
            }
            if (didResize) {
                f.validate();
            }
        }

        protected boolean inBounds(JInternalFrame f, int newX, int newY, int newWidth, int newHeight) {
            if (newX < 0 || newY < 0)
                return false;
            if (newX + newWidth > f.getDesktopPane().getWidth())
                return false;
            if (newY + newHeight > f.getDesktopPane().getHeight())
                return false;
            return true;
        }
    }
}
