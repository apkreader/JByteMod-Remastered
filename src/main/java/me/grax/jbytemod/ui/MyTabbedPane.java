package me.grax.jbytemod.ui;

import me.grax.jbytemod.JByteMod;
import de.xbrowniecodez.jbytemod.ui.lists.SearchList;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.TabbedPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MyTabbedPane extends JTabbedPane {
    private MyEditorTab editorTab;

    public MyTabbedPane(JByteMod jbm) {
        this.editorTab = new MyEditorTab(jbm);
        this.addTab("Editor", editorTab);
        SearchList searchList = new SearchList(jbm);
        jbm.setSearchList(searchList);
        JLabel search = new JLabel(JByteMod.res.getResource("search_results"));
        this.addTab(JByteMod.res.getResource("search"), this.withBorder(search, searchList));
        this.addTab("Opcodes", this.withBorder(new JLabel("Opcodes"), new OpcodeTable()));
        //MethodRefPanel mrp = new MethodRefPanel(jbm);
        //jbm.setMethodRefPanel(mrp);
        //this.addTab("References", mrp);
        jbm.setTabbedPane(this);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getButton() == 3) {
                    int tabNr = ((TabbedPaneUI) getUI()).tabForCoordinate(MyTabbedPane.this, me.getX(), me.getY());
                    if (tabNr == 0) {
                        JPopupMenu menu = new JPopupMenu();
                        for (ClassNode cn : JByteMod.instance.getLastSelectedTreeEntries().keySet()) {
                            String item = cn.name;
                            MethodNode mn = JByteMod.instance.getLastSelectedTreeEntries().get(cn);
                            if (mn != null) {
                                item += "." + mn.name;
                            }
                            if (item.length() > 128) {
                                item = "..." + item.substring(item.length() - 128);
                            }
                            JMenuItem remove = new JMenuItem(item);
                            remove.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    if (mn != null) {
                                        jbm.selectMethod(cn, mn);
                                    } else {
                                        jbm.selectClass(cn);
                                    }
                                }
                            });
                            menu.add(remove);
                        }
                        menu.show(jbm, (int) jbm.getMousePosition().getX(), (int) jbm.getMousePosition().getY());
                    }
                }
            }
        });
    }

    public void selectClass(ClassNode cn) {
        this.editorTab.selectClass(cn);
    }

    public MyEditorTab getEditorTab() {
        return editorTab;
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

    public void selectMethod(ClassNode cn, MethodNode mn) {
        this.editorTab.selectMethod(cn, mn);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(this.getWidth() / 2, 0);
    }
}
