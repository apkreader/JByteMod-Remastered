package me.grax.jbytemod.ui;

import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.ui.lists.AdressList;
import me.grax.jbytemod.ui.lists.ErrorList;
import me.grax.jbytemod.ui.lists.MyCodeList;

import javax.swing.*;
import java.awt.*;

public class MyCodeEditor extends JPanel {
    private MyCodeList cl;

    public MyCodeEditor(JByteMod jbm, JLabel editor) {
        this.setLayout(new BorderLayout());
        cl = new MyCodeList(jbm, editor);
        this.add(cl, BorderLayout.CENTER);
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("nimbusBorder")));
        p.add(new AdressList(cl), BorderLayout.CENTER);
        this.add(p, BorderLayout.WEST);
        this.add(new ErrorList(jbm, cl), BorderLayout.EAST);

    }

    public MyCodeList getEditor() {
        return cl;
    }
}
