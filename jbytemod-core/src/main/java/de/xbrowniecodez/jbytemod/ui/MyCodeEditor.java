package de.xbrowniecodez.jbytemod.ui;

import de.xbrowniecodez.jbytemod.JByteMod;
import de.xbrowniecodez.jbytemod.ui.lists.AdressList;
import me.grax.jbytemod.ui.lists.ErrorList;
import me.grax.jbytemod.ui.lists.MyCodeList;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BorderLayout;

public final class MyCodeEditor extends JPanel {
    private final MyCodeList editor;
    private final ErrorList errorList;

    public MyCodeEditor(JByteMod jByteMod, JLabel header) {
        setLayout(new BorderLayout());
        editor = new MyCodeList(jByteMod, header);
        add(editor, BorderLayout.CENTER);

        JPanel addresses = new JPanel(new BorderLayout());
        addresses.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
                UIManager.getColor("nimbusBorder")));
        addresses.add(new AdressList(editor), BorderLayout.CENTER);
        add(addresses, BorderLayout.WEST);

        errorList = new ErrorList(jByteMod, editor);
        add(errorList, BorderLayout.EAST);
    }

    public MyCodeList getEditor() {
        return editor;
    }

    public ErrorList getErrorList() {
        return errorList;
    }
}
