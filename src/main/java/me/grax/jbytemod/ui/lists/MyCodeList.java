package me.grax.jbytemod.ui.lists;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import lombok.Setter;
import me.grax.jbytemod.ui.JAnnotationEditor;
import me.grax.jbytemod.ui.JSearch;
import me.grax.jbytemod.ui.dialogue.InsnEditDialogue;
import de.xbrowniecodez.jbytemod.ui.lists.entries.FieldEntry;
import de.xbrowniecodez.jbytemod.ui.lists.entries.InstrEntry;
import de.xbrowniecodez.jbytemod.ui.lists.entries.PrototypeEntry;
import me.grax.jbytemod.utils.ErrorDisplay;
import me.grax.jbytemod.utils.HtmlSelection;
import me.grax.jbytemod.utils.list.LazyListModel;
import me.lpk.util.OpUtils;
import org.objectweb.asm.tree.*;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Setter
public class MyCodeList extends JList<InstrEntry> {
    private final JLabel editor;
    private AdressList adressList;
    private ErrorList errorList;
    private MethodNode currentMethod;
    private ClassNode currentClass;

    public MyCodeList(JByteMod jam, JLabel editor) {
        super(new LazyListModel<>());
        this.editor = editor;
        this.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        this.setFocusable(false);
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                InstrEntry entry = MyCodeList.this.getSelectedValue();
                if (entry == null) {
                    createPopupForEmptyList(jam);
                    return;
                }
                MethodNode mn = entry.getMethodNode();
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (mn != null) {
                        AbstractInsnNode ain = entry.getInsnNode();
                        rightClickMethod(jam, mn, ain, MyCodeList.this.getSelectedValuesList());
                    } else {
                        rightClickField(jam, (FieldEntry) entry, MyCodeList.this.getSelectedValuesList());
                    }
                } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    if (mn != null) {
                        try {
                            if (InsnEditDialogue.canEdit(entry.getInsnNode())) {
                                new InsnEditDialogue(mn, entry.getInsnNode()).open();
                            }
                        } catch (Exception e1) {
                            new ErrorDisplay(e1);
                        }
                    } else {
                        FieldEntry fe = (FieldEntry) entry;
                        try {
                            new InsnEditDialogue(null, fe.getFieldNode()).open();
                        } catch (Exception e1) {
                            new ErrorDisplay(e1);
                        }
                        MyCodeList.this.loadFields(fe.getClassNode());
                    }
                }
            }
        });
        InputMap im = getInputMap(WHEN_FOCUSED);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "search");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), "copy");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK), "duplicate");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK), "insert");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insert");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "down");
        am.put("search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new JSearch(MyCodeList.this).setVisible(true);
            }
        });

        am.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyToClipbord();
            }
        });

        am.put("duplicate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InstrEntry entry = getSelectedValue();
                if (entry != null && entry.getMethodNode() != null) {
                    duplicate(entry.getMethodNode(), entry.getInsnNode());
                }
            }
        });
        am.put("insert", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InstrEntry entry = getSelectedValue();
                if (entry != null && entry.getMethodNode() != null) {
                    try {
                        InsnEditDialogue.createInsertInsnDialog(entry.getMethodNode(), entry.getInsnNode(), true);
                        OpUtils.clearLabelCache();
                    } catch (Exception e1) {
                        new ErrorDisplay(e1);
                    }
                }
            }
        });
        am.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<InstrEntry> entries = getSelectedValuesList();
                for (InstrEntry entry : entries) {
                    if (entry.getMethodNode() != null) {
                        removeNode(entry.getMethodNode(), entry.getInsnNode());
                    }
                }
            }
        });
        am.put("up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InstrEntry entry = getSelectedValue();
                if (entry != null && entry.getMethodNode() != null) {
                    int index = getSelectedIndex();
                    if (moveUp(entry.getMethodNode(), entry.getInsnNode())) {
                        setSelectedIndex(index - 1);
                    }
                }
            }
        });
        am.put("down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InstrEntry entry = getSelectedValue();
                if (entry != null && entry.getMethodNode() != null) {
                    int index = getSelectedIndex();
                    if (moveDown(entry.getMethodNode(), entry.getInsnNode())) {
                        setSelectedIndex(index + 1);
                    }
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (Main.getInstance().getJByteMod().getOptions().get("hints").getBoolean()) {
                    ListModel<InstrEntry> m = getModel();
                    int index = locationToIndex(e.getPoint());
                    if (index > -1) {
                        InstrEntry el = m.getElementAt(index);
                        setToolTipText(el.getHint());
                    }
                } else {
                    setToolTipText(null);
                }
            }
        });
        this.setPrototypeCellValue(new PrototypeEntry());
        this.setFixedCellWidth(-1);
    }

    protected void rightClickField(JByteMod jbm, FieldEntry fle, List<InstrEntry> selected) {
        ClassNode cn = fle.getClassNode();
        JPopupMenu menu = new JPopupMenu();
        if (selected.size() > 1) {
            JMenuItem remove = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("remove_all"));
            remove.addActionListener(e -> {
                for (InstrEntry sel : selected) {
                    cn.fields.remove(((FieldEntry) sel).getFieldNode());
                }
                MyCodeList.this.loadFields(cn);
            });
            menu.add(remove);
            menu.add(copyText());
            menu.show(jbm, (int) jbm.getMousePosition().getX(), (int) jbm.getMousePosition().getY());
        } else {
            JMenuItem edit = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("edit"));
            edit.addActionListener(e -> {
                try {
                    new InsnEditDialogue(null, fle.getFieldNode()).open();
                } catch (Exception e1) {
                    new ErrorDisplay(e1);
                }
                MyCodeList.this.loadFields(cn);
            });
            menu.add(edit);
            JMenuItem remove = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("remove"));
            remove.addActionListener(e -> {
                cn.fields.remove(fle.getFieldNode());
                MyCodeList.this.loadFields(cn);
            });
            menu.add(remove);
            JMenuItem add = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("insert"));
            add.addActionListener(e -> {
                try {
                    FieldNode fn = new FieldNode(1, "", "", "", null);
                    if (new InsnEditDialogue(null, fn).open()) {
                        cn.fields.add(fn);
                    }
                } catch (Exception e1) {
                    new ErrorDisplay(e1);
                }
                MyCodeList.this.loadFields(cn);
            });
            menu.add(add);
            menu.add(copyText());
            JMenuItem annotations = new JMenuItem("Edit Annotations");
            annotations.addActionListener(e -> {
                if (!JAnnotationEditor.isOpen("visibleAnnotations"))
                    new JAnnotationEditor("Annotations", fle.getFieldNode(), "visibleAnnotations").setVisible(true);
            });
            menu.add(annotations);
            JMenuItem invisAnnotations = new JMenuItem("Edit Invis Annotations");
            invisAnnotations.addActionListener(e -> {
                if (!JAnnotationEditor.isOpen("invisibleAnnotations"))
                    new JAnnotationEditor("Invis Annotations", fle.getFieldNode(), "invisibleAnnotations").setVisible(true);
            });
            menu.add(invisAnnotations);
            menu.show(jbm, (int) jbm.getMousePosition().getX(), (int) jbm.getMousePosition().getY());
        }
    }

    private JMenuItem copyText() {
        JMenuItem copy = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("copy_text"));
        copy.addActionListener(e -> {
            copyToClipbord();
            Main.getInstance().getLogger().log("Copied code to clipboard!");
        });
        return copy;
    }

    protected void copyToClipbord() {
        StringBuilder sb = new StringBuilder();
        boolean html = Main.getInstance().getJByteMod().getOptions().get("copy_formatted").getBoolean();
        if (html) {
            for (InstrEntry sel : MyCodeList.this.getSelectedValuesList()) {
                sb.append(sel.toString());
                sb.append("<br>");
            }
        } else {
            for (InstrEntry sel : MyCodeList.this.getSelectedValuesList()) {
                sb.append(sel.toEasyString());
                sb.append("\n");
            }
        }
        if (sb.length() > 0) {
            HtmlSelection selection = new HtmlSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        }
    }

    protected void rightClickMethod(JByteMod jbm, MethodNode mn, AbstractInsnNode ain, List<InstrEntry> selected) {
        if (selected.size() > 1) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem remove = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("remove_all"));
            remove.addActionListener(e -> {
                for (InstrEntry sel : selected) {
                    mn.instructions.remove(sel.getInsnNode());
                }
                OpUtils.clearLabelCache();
                MyCodeList.this.loadInstructions(mn);
            });
            menu.add(remove);
            menu.add(copyText());
            addPopupListener(menu);
            menu.show(jbm, (int) jbm.getMousePosition().getX(), (int) jbm.getMousePosition().getY());
        } else {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem insertBefore = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("ins_before"));
            insertBefore.addActionListener(e -> {
                try {
                    InsnEditDialogue.createInsertInsnDialog(mn, ain, false);
                    OpUtils.clearLabelCache();
                } catch (Exception e1) {
                    new ErrorDisplay(e1);
                }
            });
            menu.add(insertBefore);
            JMenuItem insert = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("ins_after"));
            insert.addActionListener(e -> {
                try {
                    InsnEditDialogue.createInsertInsnDialog(mn, ain, true);
                    OpUtils.clearLabelCache();
                } catch (Exception e1) {
                    new ErrorDisplay(e1);
                }
            });
            insert.setAccelerator(KeyStroke.getKeyStroke('I', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            menu.add(insert);

            if (InsnEditDialogue.canEdit(ain)) {
                JMenuItem edit = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("edit"));
                edit.addActionListener(e -> {
                    try {
                        new InsnEditDialogue(mn, ain).open();
                    } catch (Exception e1) {
                        new ErrorDisplay(e1);
                    }
                });
                menu.add(edit);
            }
            if (ain instanceof JumpInsnNode) {
                JMenuItem edit = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("jump_to_label"));
                edit.addActionListener(e -> {
                    JumpInsnNode jin = (JumpInsnNode) ain;
                    ListModel<InstrEntry> model = getModel();
                    for (int i = 0; i < model.getSize(); i++) {
                        InstrEntry sel = model.getElementAt(i);
                        if (sel.getInsnNode().equals(jin.label)) {
                            setSelectedIndex(i);
                            ensureIndexIsVisible(i);
                            break;
                        }
                    }
                });
                menu.add(edit);
            }
            if (ain instanceof MethodInsnNode) {
                JMenuItem edit = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("go_to_dec"));
                JMenuItem find_usage = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("find_usage"));
                edit.addActionListener(e -> {
                    MethodInsnNode min = (MethodInsnNode) ain;
                    for (ClassNode cn : jbm.getJarArchive().getClasses().values()) {

                        if (cn.name.equals(min.owner)) {
                            for (MethodNode mn1 : cn.methods) {
                                if (min.name.equals(mn1.name) && min.desc.equals(mn1.desc)) {
                                    jbm.selectMethod(cn, mn1);
                                    jbm.treeSelection(mn1);
                                    return;
                                }
                            }
                        }
                    }
                });

                find_usage.addActionListener(e -> {
                    MethodInsnNode min = (MethodInsnNode) ain;
                    jbm.getSearchList().searchForFMInsn(min.owner, min.name, min.desc, true, false);
                });

                menu.add(edit);
                menu.add(find_usage);
            }
            if (ain instanceof FieldInsnNode) {
                JMenuItem edit = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("go_to_dec"));
                JMenuItem find_usage = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("find_usage"));
                edit.addActionListener(e -> {
                    FieldInsnNode fin = (FieldInsnNode) ain;
                    for (ClassNode cn : jbm.getJarArchive().getClasses().values()) {
                        if (cn.name.equals(fin.owner)) {
                            jbm.selectClass(cn);
                            return;
                        }
                    }
                });

                find_usage.addActionListener(e -> {
                    FieldInsnNode fin = (FieldInsnNode) ain;
                    jbm.getSearchList().searchForFMInsn(fin.owner, fin.name, fin.desc, true, true);
                });

                menu.add(edit);
                menu.add(find_usage);
            }

            JMenuItem duplicate = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("duplicate"));
            duplicate.addActionListener(e -> duplicate(mn, ain));
            duplicate.setAccelerator(KeyStroke.getKeyStroke('D', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            menu.add(duplicate);

            JMenuItem up = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("move_up"));
            up.addActionListener(e -> moveUp(mn, ain));
            up.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0));
            menu.add(up);

            JMenuItem down = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("move_down"));
            down.addActionListener(e -> moveDown(mn, ain));
            down.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0));
            menu.add(down);
            JMenuItem remove = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("remove"));
            remove.addActionListener(e -> removeNode(mn, ain));
            remove.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
            menu.add(copyText());
            menu.add(remove);
            addPopupListener(menu);
            menu.show(jbm, (int) jbm.getMousePosition().getX(), (int) jbm.getMousePosition().getY());
        }
    }

    protected void removeNode(MethodNode mn, AbstractInsnNode ain) {
        mn.instructions.remove(ain);
        OpUtils.clearLabelCache();
        loadInstructions(mn);
    }

    protected boolean moveDown(MethodNode mn, AbstractInsnNode ain) {
        AbstractInsnNode node = ain.getNext();
        if (node != null) {
            mn.instructions.remove(node);
            mn.instructions.insertBefore(ain, node);
            OpUtils.clearLabelCache();
            loadInstructions(mn);
            return true;
        }
        return false;
    }

    protected boolean moveUp(MethodNode mn, AbstractInsnNode ain) {
        AbstractInsnNode node = ain.getPrevious();
        if (node != null) {
            mn.instructions.remove(node);
            mn.instructions.insert(ain, node);
            OpUtils.clearLabelCache();
            loadInstructions(mn);
            return true;
        }
        return false;
    }

    protected void duplicate(MethodNode mn, AbstractInsnNode ain) {
        try {
            if (ain instanceof LabelNode) {
                mn.instructions.insert(ain, new LabelNode());
                OpUtils.clearLabelCache();
            } else if (ain instanceof JumpInsnNode) {
                mn.instructions.insert(ain, new JumpInsnNode(ain.getOpcode(), ((JumpInsnNode) ain).label));
            } else {
                mn.instructions.insert(ain, ain.clone(new HashMap<>()));
            }
            MyCodeList.this.loadInstructions(mn);

        } catch (Exception e1) {
            new ErrorDisplay(e1);
        }
    }

    protected void createPopupForEmptyList(JByteMod jbm) {
        JPopupMenu menu = new JPopupMenu();
        if (currentMethod != null) {
            JMenuItem add = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("add"));
            add.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        InsnEditDialogue.createInsertInsnDialog(currentMethod, null, true);
                    } catch (Exception e1) {
                        new ErrorDisplay(e1);
                    }

                }
            });
            menu.add(add);
        } else if (currentClass != null) {
            JMenuItem add = new JMenuItem(Main.getInstance().getJByteMod().getLanguageRes().getResource("add"));
            add.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        FieldNode fn = new FieldNode(1, "", "", "", null);
                        if (new InsnEditDialogue(null, fn).open()) {
                            currentClass.fields.add(fn);
                        }
                    } catch (Exception e1) {
                        new ErrorDisplay(e1);
                    }
                    MyCodeList.this.loadFields(currentClass);
                }

            });
            menu.add(add);
        }
        try {
            menu.show(jbm, (int) jbm.getMousePosition().getX(), (int) jbm.getMousePosition().getY());
        } catch (NullPointerException exception) {
             Main.getInstance().getLogger().println("Null mouse position, weird. :/");
        }

    }

    protected void addPopupListener(JPopupMenu menu) {
        menu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                MyCodeList.this.setFocusable(true);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                MyCodeList.this.setFocusable(true);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                MyCodeList.this.setFocusable(false);
            }
        });
    }

    public boolean loadInstructions(MethodNode m) {
        this.currentMethod = m;
        this.currentClass = null;
        LazyListModel<InstrEntry> lm = new LazyListModel<InstrEntry>();
        editor.setText(m.name + m.desc);
        ArrayList<InstrEntry> entries = new ArrayList<>();
        for (AbstractInsnNode i : m.instructions) {
            InstrEntry entry = new InstrEntry(m, i);
            lm.addElement(entry);
            entries.add(entry);
        }
        this.setModel(lm);
        //update sidebar
        if (adressList != null) {
            adressList.updateAdr();
        }
        if (errorList != null) {
            errorList.updateErrors();
        }
        return true;
    }

    public boolean loadFields(ClassNode cn) {
        if(cn.fields.isEmpty())
            return false;
        this.currentClass = cn;
        this.currentMethod = null;
        LazyListModel<InstrEntry> lm = new LazyListModel<>();
        String crashFixClassName = cn.name.replace("<html>", "HTMLCrashtag");
        editor.setText(crashFixClassName + " Fields");
        ArrayList<InstrEntry> entries = new ArrayList<>();
        for (FieldNode fn : cn.fields) {
            InstrEntry entry = new FieldEntry(cn, fn);
            lm.addElement(entry);
            entries.add(entry);
        }
        this.setModel(lm);
        //update sidebar
        if (adressList != null) {
            adressList.updateAdr();
        }
        if (errorList != null) {
            errorList.updateErrors();
        }
        return true;
    }
}
