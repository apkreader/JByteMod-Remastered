package de.xbrowniecodez.jbytemod;

import com.sun.tools.attach.VirtualMachine;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import de.xbrowniecodez.jbytemod.utils.Utils;
import de.xbrowniecodez.jbytemod.utils.update.objects.Version;
import lombok.Getter;
import lombok.Setter;
import de.xbrowniecodez.jbytemod.plugin.Plugin;
import de.xbrowniecodez.jbytemod.plugin.PluginManager;
import me.grax.jbytemod.JarArchive;
import me.grax.jbytemod.res.LanguageRes;
import me.grax.jbytemod.res.Options;
import me.grax.jbytemod.ui.*;
import de.xbrowniecodez.jbytemod.ui.lists.LVPList;
import me.grax.jbytemod.ui.lists.MyCodeList;
import de.xbrowniecodez.jbytemod.ui.lists.SearchList;
import de.xbrowniecodez.jbytemod.ui.lists.TCBList;
import me.grax.jbytemod.ui.tree.SortedTreeNode;
import me.grax.jbytemod.utils.ErrorDisplay;
import me.grax.jbytemod.utils.gui.LookUtils;
import me.grax.jbytemod.utils.task.AttachTask;
import me.grax.jbytemod.utils.task.RetransformTask;
import me.grax.jbytemod.utils.task.SaveTask;
import me.lpk.util.OpUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;

@Getter
@Setter
public class JByteMod extends JFrame {
    private final Version version = new Version(Utils.readPropertiesFile().getProperty("version"));
    private final String title = "JByteMod Remastered v" + version;
    private LanguageRes languageRes;
    private String lastEditFile = "";
    private HashMap<ClassNode, MethodNode> lastSelectedTreeEntries = new LinkedHashMap<>();
    private JarArchive jarArchive;
    private Instrumentation agentInstrumentation;
    private Options options;
    private ClassTree jarTree;
    private MyCodeList codeList;
    private PageEndPanel pageEndPanel;
    private SearchList searchList;
    private DecompilerPanel decompilerPanel;
    private TCBList tcbList;
    private MyTabbedPane tabbedPane;
    private InfoPanel infoPanel;
    private LVPList lvpList;
    private MyMenuBar myMenuBar;
    private ClassNode currentNode;
    private MethodNode currentMethod;
    private PluginManager pluginManager;
    private File filePath;

    public JByteMod(boolean agent) throws Exception {
        this.options = new Options();
        this.languageRes = new LanguageRes();
    }

    public void initializeFrame(boolean agent) {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                handleWindowClosing(agent);
            }
        });

        setBounds(100, 100, 1280, 720);
        setTitle(title);
        setJMenuBar(myMenuBar = new MyMenuBar(this, agent));
        jarTree = new ClassTree(this);
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(5, 5));
        setContentPane(contentPane);
        setTcbList(new TCBList());
        setLvpList(new LVPList());
        createSplitPane(contentPane);
        contentPane.add(pageEndPanel = new PageEndPanel(), BorderLayout.PAGE_END);
        contentPane.add(new MyToolBar(this), BorderLayout.PAGE_START);

        if (jarArchive != null) {
            refreshTree();
        }
    }

    private void createSplitPane(Container container) {
        JPanel borderPanel = new JPanel();
        borderPanel.setBorder(null);
        borderPanel.setLayout(new GridLayout());
        JSplitPane splitPane = new MySplitPane(this, jarTree);
        JPanel b2 = new JPanel();
        b2.setBorder(new EmptyBorder(5, 0, 5, 0));
        b2.setLayout(new GridLayout());
        b2.add(splitPane);
        borderPanel.add(b2);
        container.add(borderPanel, BorderLayout.CENTER);
    }

    private void handleWindowClosing(boolean agent) {
        if (JOptionPane.showConfirmDialog(JByteMod.this, languageRes.getResource("exit_warn"), languageRes.getResource("is_sure"),
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (agent) {
                dispose();
            } else {
                Main.INSTANCE.getDiscord().shutdown();
                Runtime.getRuntime().exit(0);
            }
        }
    }

    public void applyChangesAgent() {
        if (agentInstrumentation == null) {
            throw new RuntimeException();
        }
        new RetransformTask(this, agentInstrumentation, jarArchive).execute();
    }

    public void attachTo(VirtualMachine vm) throws Exception {
        new AttachTask(this, vm).execute();
    }

    /**
     * Load .jar or .class file
     */
    public void loadFile(File input) {
        this.filePath = input;
        String ap = input.getAbsolutePath();

        try {
            if (ap.endsWith(".jar")) {
                loadJarFile(input);
            } else if (ap.endsWith(".class")) {
                loadClassFile(input);
            } else {
                displayJarWarning();
            }

            notifyPlugins();
        } catch (Throwable e) {
            new ErrorDisplay(e);
        }
    }

    private void loadJarFile(File input) {
        jarArchive = new JarArchive(this, input);
        setTitleSuffix(input.getName());
    }

    private void loadClassFile(File input) throws Exception {
        jarArchive = new JarArchive(BytecodeUtils.getClassNodeFromBytes(Files.readAllBytes(input.toPath())));
        setTitleSuffix(input.getName());
        refreshTree();
    }

    private void displayJarWarning() {
        new ErrorDisplay(new UnsupportedOperationException(languageRes.getResource("jar_warn")));
    }

    private void notifyPlugins() {
        for (Plugin p : pluginManager.getPlugins()) {
            p.loadFile(jarArchive.getClasses());
        }
    }

    public void refreshAgentClasses() {
        if (agentInstrumentation == null) {
            throw new RuntimeException();
        }
        this.refreshTree();
    }

    public void refreshTree() {
        Main.INSTANCE.getLogger().log("Building tree..");
        this.jarTree.refreshTree(jarArchive);
    }

    public void saveFile(File output) {
        try {
            new SaveTask(this, output, jarArchive).execute();
        } catch (Throwable t) {
            new ErrorDisplay(t);
        }
    }

    public void selectClass(ClassNode cn) {
        if (Main.INSTANCE.getJByteMod().getOptions().get("select_code_tab").getBoolean()) {
            tabbedPane.setSelectedIndex(0);
        }
        this.currentNode = cn;
        this.currentMethod = null;
        infoPanel.selectClass(cn);
        codeList.loadFields(cn);
        tabbedPane.selectClass(cn);
        lastSelectedTreeEntries.put(cn, null);
        if (lastSelectedTreeEntries.size() > 5) {
            lastSelectedTreeEntries.remove(lastSelectedTreeEntries.keySet().iterator().next());
        }
    }

    private boolean selectEntry(MethodNode mn, DefaultTreeModel tm, SortedTreeNode node) {
        for (int i = 0; i < tm.getChildCount(node); i++) {
            SortedTreeNode child = (SortedTreeNode) tm.getChild(node, i);
            if (child.getMethodNode() != null && child.getMethodNode().equals(mn)) {
                TreePath tp = new TreePath(tm.getPathToRoot(child));
                jarTree.setSelectionPath(tp);
                jarTree.scrollPathToVisible(tp);
                return true;
            }
            if (!child.isLeaf()) {
                if (selectEntry(mn, tm, child)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void selectMethod(ClassNode cn, MethodNode mn) {
        if (Main.INSTANCE.getJByteMod().getOptions().get("select_code_tab").getBoolean()) {
            tabbedPane.setSelectedIndex(0);
        }
        OpUtils.clearLabelCache();
        this.currentNode = cn;
        this.currentMethod = mn;
        infoPanel.selectMethod(cn, mn);
        if (!codeList.loadInstructions(mn)) {
            codeList.setSelectedIndex(-1);
        }
        tcbList.addNodes(cn, mn);
        lvpList.addNodes(cn, mn);
        decompilerPanel.setText("");
        tabbedPane.selectMethod(cn, mn);
        lastSelectedTreeEntries.put(cn, mn);
        if (lastSelectedTreeEntries.size() > 5) {
            lastSelectedTreeEntries.remove(lastSelectedTreeEntries.keySet().iterator().next());
        }
    }

    private void setTitleSuffix(String suffix) {
        this.setTitle(title + " - " + suffix);
    }

    @Override
    public void setVisible(boolean b) {
        LookUtils.setTheme();
        this.initializeFrame(false);
        this.setPluginManager(new PluginManager(this));
        this.myMenuBar.addPluginMenu(pluginManager.getPlugins());
        super.setVisible(b);
    }

    public void treeSelection(ClassNode cn, MethodNode mn) {
        //selection may take some time
        new Thread(() -> {
            DefaultTreeModel tm = (DefaultTreeModel) jarTree.getModel();
            if (this.selectEntry(mn, tm, (SortedTreeNode) tm.getRoot())) {
                jarTree.repaint();
            }
        }).start();
    }
}
