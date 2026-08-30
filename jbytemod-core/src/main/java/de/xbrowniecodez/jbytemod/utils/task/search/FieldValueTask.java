package de.xbrowniecodez.jbytemod.utils.task.search;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.ui.PageEndPanel;
import de.xbrowniecodez.jbytemod.ui.lists.SearchList;
import de.xbrowniecodez.jbytemod.ui.lists.entries.SearchEntry;
import me.grax.jbytemod.utils.TextUtils;
import me.grax.jbytemod.utils.list.LazyListModel;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class FieldValueTask extends SwingWorker<Void, Integer> {

    private PageEndPanel jpb;
    private JByteMod jbm;
    private String value;
    private boolean exact;
    private boolean caseSens;
    private Pattern pattern;
    private SearchList sl;

    public FieldValueTask(SearchList sl, JByteMod jbm, String value, boolean exact, boolean caseSens, boolean regex) {
        this.sl = sl;
        this.jbm = jbm;
        this.jpb = jbm.getPageEndPanel();
        this.exact = exact;
        this.caseSens = caseSens;
        if (regex) {
            this.pattern = Pattern.compile(value);
        }

        if (!caseSens) {
            this.value = value.toLowerCase();
        } else {
            this.value = value;
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        LazyListModel<SearchEntry> model = new LazyListModel<>();
        Collection<ClassNode> values = jbm.getJarArchive().getClasses().values();
        double size = values.size();
        double i = 0;
        boolean exact = this.exact;
        boolean regex = this.pattern != null;
        for (ClassNode cn : values) {
            for (FieldNode fn : cn.fields) {
                if (fn.value != null) {
                    String valStr = fn.value.toString();
                    String valStrLower = caseSens ? valStr : valStr.toLowerCase();
                    if (regex ? pattern.matcher(valStr).matches() : (exact ? valStrLower.equals(value) : valStrLower.contains(value))) {
                        model.addElement(new SearchEntry(cn, fn, TextUtils.escape(TextUtils.max(valStr, 100))));
                    }
                }
            }
            publish(Math.min((int) (i++ / size * 100d) + 1, 100));
        }
        sl.setModel(model);
        publish(100);
        return null;
    }

    @Override
    protected void process(List<Integer> chunks) {
        int i = chunks.get(chunks.size() - 1);
        jpb.setValue(i);
        super.process(chunks);
    }

    @Override
    protected void done() {
        Main.INSTANCE.getLogger().log("Field value search finished!");
    }
}
