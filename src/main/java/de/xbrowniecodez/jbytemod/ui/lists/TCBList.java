package de.xbrowniecodez.jbytemod.ui.lists;

import de.xbrowniecodez.jbytemod.Main;
import me.grax.jbytemod.ui.dialogue.InsnEditDialogue;
import de.xbrowniecodez.jbytemod.ui.lists.entries.TCBEntry;
import me.grax.jbytemod.utils.ErrorDisplay;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class TCBList extends JList<TCBEntry> {

    private ClassNode currentClassNode;
    private MethodNode currentMethodNode;

    public void addNodes(ClassNode cn, MethodNode mn) {
        this.currentClassNode = cn;
        this.currentMethodNode = mn;
        DefaultListModel<TCBEntry> model = createListModel(mn);
        setModel(model);
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopupMenu(e);
                }
            }
        });
    }

    private DefaultListModel<TCBEntry> createListModel(MethodNode mn) {
        DefaultListModel<TCBEntry> model = new DefaultListModel<>();
        for (TryCatchBlockNode tcbn : mn.tryCatchBlocks) {
            model.addElement(new TCBEntry(currentClassNode, mn, tcbn));
        }
        return model;
    }

    private void showPopupMenu(MouseEvent e) {
        List<TCBEntry> selectedEntries = getSelectedValuesList();
        JPopupMenu menu = new JPopupMenu();
        if (!selectedEntries.isEmpty()) {
            JMenuItem removeItem = new JMenuItem(selectedEntries.size() > 1 ? Main.getInstance().getJByteMod().getLanguageRes().getResource("remove_all") : Main.getInstance().getJByteMod().getLanguageRes().getResource("remove"));
            removeItem.addActionListener(this::removeSelectedEntries);
            menu.add(removeItem);

            JMenuItem editItem = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("edit"));
            editItem.addActionListener(this::editSelectedEntries);
            menu.add(editItem);
        }

        JMenuItem insertItem = new JMenuItem(getSelectedValuesList().isEmpty() ?
                Main.getInstance().getJByteMod().getLanguageRes().getResource("add") : Main.getInstance().getJByteMod().getLanguageRes().getResource("insert"));
        insertItem.addActionListener(this::insertEntry);
        menu.add(insertItem);

        menu.show(this, e.getX(), e.getY());
    }

    private void removeSelectedEntries(ActionEvent e) {
        List<TCBEntry> selectedEntries = getSelectedValuesList();
        if (selectedEntries.isEmpty()) return;

        for (TCBEntry selected : selectedEntries) {
            try {
                currentMethodNode.tryCatchBlocks.remove(selected.getTcbn());
            } catch (Exception ex) {
                new ErrorDisplay(ex);
            }
        }
        refreshList();
    }

    private void editSelectedEntries(ActionEvent e) {
        List<TCBEntry> selectedEntries = getSelectedValuesList();
        if (selectedEntries.isEmpty()) return;

        for (TCBEntry selected : selectedEntries) {
            try {
                TryCatchBlockNode tcbn = selected.getTcbn();
                if (tcbn.type == null) {
                    tcbn.type = "";
                }
                new InsnEditDialogue(currentMethodNode, tcbn).open();
                if (tcbn.type != null && tcbn.type.isEmpty()) {
                    tcbn.type = null;
                }
            } catch (Exception ex) {
                new ErrorDisplay(ex);
            }
        }
        refreshList();
    }

    private void insertEntry(ActionEvent e) {
        TryCatchBlockNode tcbn = new TryCatchBlockNode(null, null, null, "");
        if (new InsnEditDialogue(currentMethodNode, tcbn).open()) {
            if (tcbn.handler != null && tcbn.start != null && tcbn.end != null) {
                if (tcbn.type != null && tcbn.type.isEmpty()) {
                    tcbn.type = null;
                }
                currentMethodNode.tryCatchBlocks.add(tcbn);
                refreshList();
            }
        }
    }

    private void refreshList() {
        addNodes(currentClassNode, currentMethodNode);
    }
}
