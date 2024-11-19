package me.grax.jbytemod.utils.task.search;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.ui.PageEndPanel;
import de.xbrowniecodez.jbytemod.ui.lists.SearchList;
import me.grax.jbytemod.ui.lists.entries.SearchEntry;
import me.grax.jbytemod.utils.list.LazyListModel;
import org.objectweb.asm.tree.*;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

public class ReferenceTask extends SwingWorker<Void, Integer> {

    private final SearchList searchList;
    private final PageEndPanel progressBar;
    private final JByteMod jByteMod;
    private final boolean exactMatch;
    private final String owner;
    private final String name;
    private final String descriptor;
    private final boolean isFieldSearch;

    public ReferenceTask(SearchList searchList, JByteMod jByteMod, String owner, String name, String descriptor, boolean exactMatch, boolean isFieldSearch) {
        this.searchList = searchList;
        this.jByteMod = jByteMod;
        this.progressBar = jByteMod.getPageEndPanel();
        this.exactMatch = exactMatch;
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.isFieldSearch = isFieldSearch;
    }

    @Override
    protected Void doInBackground() throws Exception {
        LazyListModel<SearchEntry> model = new LazyListModel<>();
        Collection<ClassNode> classes = jByteMod.getJarArchive().getClasses().values();
        double totalClasses = classes.size();
        int processedClasses = 0;

        for (ClassNode classNode : classes) {
            for (MethodNode methodNode : classNode.methods) {
                processMethod(classNode, methodNode, model);
            }
            publish(getProgressPercentage(++processedClasses, totalClasses));
        }

        searchList.setModel(model);
        publish(100); // Ensure progress is set to 100% on completion
        return null;
    }

    private void processMethod(ClassNode classNode, MethodNode methodNode, LazyListModel<SearchEntry> model) {
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (isFieldSearch && instruction.getType() == AbstractInsnNode.FIELD_INSN) {
                FieldInsnNode fin = (FieldInsnNode) instruction;
                if (matches(fin.owner, fin.name, fin.desc)) {
                    model.addElement(new SearchEntry(classNode, methodNode, fin));
                }
            } else if (!isFieldSearch) {
                if (instruction.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode min = (MethodInsnNode) instruction;
                    if (matches(min.owner, min.name, min.desc)) {
                        model.addElement(new SearchEntry(classNode, methodNode, min));
                    }
                } else if (instruction.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
                    InvokeDynamicInsnNode idn = (InvokeDynamicInsnNode) instruction;
                    if (matchesDynamic(idn)) {
                        model.addElement(new SearchEntry(classNode, methodNode, idn));
                    }
                }
            }
        }
    }

    private boolean matches(String instructionOwner, String instructionName, String instructionDesc) {
        if (exactMatch) {
            return owner.equals(instructionOwner) && name.equals(instructionName) && descriptor.equals(instructionDesc);
        }
        return instructionOwner.contains(owner) && instructionName.contains(name) && instructionDesc.contains(descriptor);
    }

    private boolean matchesDynamic(InvokeDynamicInsnNode idn) {
        if (exactMatch) {
            return idn.name.equals(name) && idn.desc.equals(descriptor);
        }
        return idn.name.contains(name) && idn.desc.contains(descriptor);
    }

    private int getProgressPercentage(double current, double total) {
        return Math.min((int) (current / total * 100), 100);
    }

    @Override
    protected void process(List<Integer> chunks) {
        int progress = chunks.get(chunks.size() - 1);
        progressBar.setValue(progress);
    }

    @Override
    protected void done() {
        progressBar.setValue(100);
        Main.INSTANCE.getLogger().log("Search finished!");
    }
}
