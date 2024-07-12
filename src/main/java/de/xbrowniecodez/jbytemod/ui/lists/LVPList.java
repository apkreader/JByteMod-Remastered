package de.xbrowniecodez.jbytemod.ui.lists;

import de.xbrowniecodez.jbytemod.Main;
import me.grax.jbytemod.ui.dialogue.InsnEditDialogue;
import me.grax.jbytemod.ui.lists.entries.LVPEntry;
import me.grax.jbytemod.utils.ErrorDisplay;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class LVPList extends JList<LVPEntry> {

    private ClassNode currentClassNode;
    private MethodNode currentMethodNode;

    public void addNodes(ClassNode cn, MethodNode mn) {
        this.currentClassNode = cn;
        this.currentMethodNode = mn;
        DefaultListModel<LVPEntry> model = createListModel(mn);
        setModel(model);
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Allow multiple selections
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopupMenu(e);
                }
            }
        });
    }

    private DefaultListModel<LVPEntry> createListModel(MethodNode mn) {
        DefaultListModel<LVPEntry> model = new DefaultListModel<>();
        if (mn.localVariables != null) {
            for (LocalVariableNode lvn : mn.localVariables) {
                model.addElement(new LVPEntry(currentClassNode, mn, lvn));
            }
        }
        return model;
    }

    private void showPopupMenu(MouseEvent e) {
        List<LVPEntry> selectedEntries = getSelectedValuesList();
        JPopupMenu menu = new JPopupMenu();

        if (!selectedEntries.isEmpty()) {
            JMenuItem remove = new JMenuItem(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("remove"));
            remove.addActionListener(this::removeSelectedEntries);
            menu.add(remove);

            JMenuItem edit = new JMenuItem(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("edit"));
            edit.addActionListener(this::editSelectedEntries);
            menu.add(edit);
        }

        JMenuItem insert = new JMenuItem(selectedEntries.isEmpty() ?
                Main.INSTANCE.getJByteMod().getLanguageRes().getResource("add") : Main.INSTANCE.getJByteMod().getLanguageRes().getResource("insert"));
        insert.addActionListener(this::insertEntry);
        menu.add(insert);

        menu.show(this, e.getX(), e.getY());
    }

    private void removeSelectedEntries(ActionEvent e) {
        if (currentMethodNode == null) return;
        List<LVPEntry> selectedEntries = getSelectedValuesList();
        if (selectedEntries.isEmpty()) return;

        for (LVPEntry selected : selectedEntries) {
            try {
                currentMethodNode.localVariables.remove(selected.getLvn());
            } catch (Exception ex) {
                new ErrorDisplay(ex);
            }
        }
        refreshList();
    }

    private void editSelectedEntries(ActionEvent e) {
        if (currentMethodNode == null) return;
        List<LVPEntry> selectedEntries = getSelectedValuesList();
        if (selectedEntries.isEmpty()) return;

        for (LVPEntry selected : selectedEntries) {
            try {
                new InsnEditDialogue(currentMethodNode, selected.getLvn()).open();
            } catch (Exception ex) {
                new ErrorDisplay(ex);
            }
        }
        refreshList();
    }

    private void insertEntry(ActionEvent e) {
        if (currentMethodNode == null) return;
        LocalVariableNode lvn = new LocalVariableNode("", "", "", null, null, currentMethodNode.localVariables.size());
        try {
            if (new InsnEditDialogue(currentMethodNode, lvn).open()) {
                if (lvn.start != null && lvn.end != null) {
                    currentMethodNode.localVariables.add(lvn);
                    refreshList();
                }
            }
        } catch (Exception ex) {
            new ErrorDisplay(ex);
        }
    }

    private void refreshList() {
        addNodes(currentClassNode, currentMethodNode);
    }
}
