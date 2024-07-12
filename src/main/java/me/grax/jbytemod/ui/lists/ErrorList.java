package me.grax.jbytemod.ui.lists;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.analysis.errors.EmptyMistake;
import me.grax.jbytemod.analysis.errors.ErrorAnalyzer;
import me.grax.jbytemod.analysis.errors.Mistake;
import me.grax.jbytemod.ui.lists.entries.InstrEntry;
import me.grax.jbytemod.utils.list.LazyListModel;
import org.objectweb.asm.tree.AbstractInsnNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

public class ErrorList extends JList<Mistake> {
    private MyCodeList cl;
    private ImageIcon warning;
    private ListCellRenderer<? super Mistake> oldRenderer;
    private JByteMod jbm;

    public ErrorList(JByteMod jbm, MyCodeList cl) {
        super(new DefaultListModel<Mistake>());
        this.jbm = jbm;
        this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        this.warning = new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/warning.png")));
        this.cl = cl;
        cl.setErrorList(this);
        this.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(-1, -1);
            }
        });
        this.oldRenderer = this.getCellRenderer();
        this.setCellRenderer(new CustomCellRenderer());
        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                Mistake error = getModel().getElementAt(index);
                if (!(error instanceof EmptyMistake)) {
                    showPopover(error, e.getXOnScreen(), e.getYOnScreen());
                }
            }
        });
        this.updateErrors();
        //SwingUtils.disableSelection(this);
    }

    private void showPopover(Mistake error, int x, int y) {
        JPopupMenu popover = new JPopupMenu();

        // Create a custom panel to display the error message
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel(error.getDesc()));

        popover.add(panel);

        // Show the popover at the specified position
        popover.show(jbm, x - jbm.getLocationOnScreen().x, y - jbm.getLocationOnScreen().y);
    }


    public void updateErrors() {
        if (Main.INSTANCE.getJByteMod().getOptions().get("analyze_errors").getBoolean() && jbm.getCurrentMethod() != null) {
            LazyListModel<Mistake> lm = new LazyListModel<Mistake>();
            LazyListModel<InstrEntry> clm = (LazyListModel<InstrEntry>) cl.getModel();
            if (clm.getSize() > 1000) {
                 Main.INSTANCE.getLogger().warn("Not analyzing mistakes, too many instructions!");
                return;
            }
            ErrorAnalyzer ea = new ErrorAnalyzer(jbm.getCurrentNode(), jbm.getCurrentMethod());
            HashMap<AbstractInsnNode, Mistake> mistakes = ea.findErrors();
            for (int i = 0; i < clm.getSize(); i++) {
                AbstractInsnNode ain = clm.getElementAt(i).getInstr();
                if (mistakes.containsKey(ain)) {
                    lm.addElement(mistakes.get(ain));
                } else {
                    lm.addElement(new EmptyMistake());
                }
            }
            this.setModel(lm);
        } else {
            this.setModel(new LazyListModel<Mistake>());
        }
    }

    class CustomCellRenderer extends JLabel implements ListCellRenderer<Mistake> {
        public Component getListCellRendererComponent(JList<? extends Mistake> list, Mistake value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = oldRenderer.getListCellRendererComponent(list, value, index, false, false); //hacky hack
            JLabel label = (JLabel) c;
            if (value.getDesc().length() > 1) {
                label.setIcon(warning);
            }
            label.setText("\u200B"); //another hacky hack
            return c;
        }
    }
}
