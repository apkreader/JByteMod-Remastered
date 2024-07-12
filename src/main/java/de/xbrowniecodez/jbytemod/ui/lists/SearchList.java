package de.xbrowniecodez.jbytemod.ui.lists;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.ui.lists.entries.SearchEntry;
import me.grax.jbytemod.utils.list.LazyListModel;
import me.grax.jbytemod.utils.task.search.LdcTask;
import me.grax.jbytemod.utils.task.search.ReferenceTask;
import me.grax.jbytemod.utils.task.search.SFTask;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

public class SearchList extends JList<SearchEntry> {

    private final JByteMod jByteMod;

    public SearchList(JByteMod jByteMod) {
        super(new LazyListModel<>());
        this.jByteMod = jByteMod;
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopupMenu(e);
                }
            }
        });
        setPrototypeCellValue(new SearchEntry());
    }

    private void showPopupMenu(MouseEvent e) {
        SearchEntry selectedEntry = getSelectedValue();
        if (selectedEntry == null) return;

        JPopupMenu menu = new JPopupMenu();
        JMenuItem goToDeclarationItem = new JMenuItem(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("go_to_dec"));
        goToDeclarationItem.addActionListener(createGoToDeclarationAction(selectedEntry));
        menu.add(goToDeclarationItem);

        JMenuItem selectTreeItem = new JMenuItem(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("select_tree"));
        selectTreeItem.addActionListener(createSelectTreeAction(selectedEntry));
        menu.add(selectTreeItem);

        JMenuItem copyTextItem = new JMenuItem(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("copy_text"));
        copyTextItem.addActionListener(createCopyTextAction(selectedEntry));
        menu.add(copyTextItem);

        menu.show(this, e.getX(), e.getY());
    }

    private ActionListener createGoToDeclarationAction(SearchEntry selectedEntry) {
        return e -> {
            ClassNode cn = selectedEntry.getCn();
            MethodNode mn = selectedEntry.getMn();
            jByteMod.selectMethod(cn, mn);
        };
    }

    private ActionListener createSelectTreeAction(SearchEntry selectedEntry) {
        return e -> {
            ClassNode cn = selectedEntry.getCn();
            MethodNode mn = selectedEntry.getMn();
            jByteMod.treeSelection(cn, mn);
        };
    }

    private ActionListener createCopyTextAction(SearchEntry selectedEntry) {
        return e -> {
            StringSelection selection = new StringSelection(selectedEntry.getFound());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        };
    }

    public void searchForConstant(String ldc, boolean exact, boolean cs, boolean regex) {
        new LdcTask(this, jByteMod, ldc, exact, cs, regex).execute();
    }

    public void searchForPatternRegex(Pattern p) {
        new LdcTask(this, jByteMod, p).execute();
    }

    public void searchForFMInsn(String owner, String name, String desc, boolean exact, boolean field) {
        new ReferenceTask(this, jByteMod, owner, name, desc, exact, field).execute();
    }

    public void searchForSF(String sf) {
        new SFTask(this, jByteMod, sf).execute();
    }
}
