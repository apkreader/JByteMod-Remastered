package me.grax.jbytemod;

import com.sun.tools.attach.VirtualMachine;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import de.xbrowniecodez.jbytemod.utils.Utils;
import de.xbrowniecodez.jbytemod.utils.update.UpdateChecker;
import de.xbrowniecodez.jbytemod.utils.update.objects.Version;
import lombok.Getter;
import lombok.Setter;
import me.grax.jbytemod.discord.Discord;
import me.grax.jbytemod.logging.Logging;
import de.xbrowniecodez.jbytemod.plugin.Plugin;
import de.xbrowniecodez.jbytemod.plugin.PluginManager;
import me.grax.jbytemod.res.LanguageRes;
import me.grax.jbytemod.res.Options;
import me.grax.jbytemod.ui.*;
import de.xbrowniecodez.jbytemod.ui.lists.LVPList;
import me.grax.jbytemod.ui.lists.MyCodeList;
import de.xbrowniecodez.jbytemod.ui.lists.SearchList;
import de.xbrowniecodez.jbytemod.ui.lists.TCBList;
import me.grax.jbytemod.ui.tree.SortedTreeNode;
import me.grax.jbytemod.utils.ErrorDisplay;
import me.grax.jbytemod.utils.FileUtils;
import me.grax.jbytemod.utils.gui.LookUtils;
import me.grax.jbytemod.utils.task.AttachTask;
import me.grax.jbytemod.utils.task.RetransformTask;
import me.grax.jbytemod.utils.task.SaveTask;
import me.lpk.util.OpUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
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
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class JByteMod extends JFrame {
	public static final Version version = new Version(Utils.readPropertiesFile().getProperty("version"));
    private static final String jbytemod = "JByteMod Remastered v" + version;
    
    public static File workingDir = new File(".");
    public static String configPath = "jbyte-remastered.cfg";
    public static Logging LOGGER;
    public static LanguageRes res;
    public static Options ops;
    public static String lastEditFile = "";
    public static HashMap<ClassNode, MethodNode> lastSelectedTreeEntries = new LinkedHashMap<>();
    public static JByteMod instance;
    public static Color border;
    private static boolean lafInit;
    @Getter
    private JarArchive jarArchive;
    private Instrumentation agentInstrumentation;

    static {
        try {
            System.loadLibrary("attach");
        } catch (Throwable ex) {

        }
    }

    private JPanel contentPane;
    @Getter
    private ClassTree jarTree;
    @Getter
    @Setter
    private MyCodeList codeList;
    @Getter
    private PageEndPanel pageEndPanel;
    @Getter
    @Setter
    private SearchList searchList;
    @Getter
    private DecompilerPanel dp;
    @Getter
    @Setter
    private TCBList tcbList;
    @Getter
    @Setter
    private MyTabbedPane tabbedPane;
    private InfoPanel sp;
    @Getter
    @Setter
    private LVPList lvpList;
    @Getter
    private MyMenuBar myMenuBar;
    @Getter
    private ClassNode currentNode;
    @Getter
    private MethodNode currentMethod;
    @Getter
    @Setter
    private PluginManager pluginManager;
    @Getter
    private File filePath;
    @Getter
    private final Discord discord = new Discord("1184572566795468881");

    /**
     * Create the frame.
     * @throws Exception 
     */
    public JByteMod(boolean agent) throws Exception {
        discord.init();
        initialize();
        initializeComponents(agent);
    }

    public static void initialize() {
        LOGGER = new Logging();
        res = new LanguageRes();
        ops = new Options();

        try {
            System.setProperty("file.encoding", "UTF-8");
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Throwable t) {
            LOGGER.err("Failed to set encoding to UTF-8 (" + t.getMessage() + ")");
        }
    }

    private void initializeComponents(boolean agent) throws Exception {
        new UpdateChecker();
        initializeFrame(agent);
    }

    public void initializeFrame(boolean agent) {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                handleWindowClosing(agent);
            }
        });

        border = UIManager.getColor("nimbusBorder");
        if (border == null) {
            border = new Color(146, 151, 161);
        }

        setBounds(100, 100, 1280, 720);
        setTitle(jbytemod);
        setJMenuBar(myMenuBar = new MyMenuBar(this, agent));
        jarTree = new ClassTree(this);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(5, 5));
        setContentPane(contentPane);
        setTcbList(new TCBList());
        setLvpList(new LVPList());
        createSplitPane();
        contentPane.add(pageEndPanel = new PageEndPanel(), BorderLayout.PAGE_END);
        contentPane.add(new MyToolBar(this), BorderLayout.PAGE_START);

        if (jarArchive != null) {
            refreshTree();
        }
    }

    private void createSplitPane() {
        JPanel borderPanel = new JPanel();
        borderPanel.setBorder(null);
        borderPanel.setLayout(new GridLayout());
        JSplitPane splitPane = new MySplitPane(this, jarTree);
        JPanel b2 = new JPanel();
        b2.setBorder(new EmptyBorder(5, 0, 5, 0));
        b2.setLayout(new GridLayout());
        b2.add(splitPane);
        borderPanel.add(b2);
        contentPane.add(borderPanel, BorderLayout.CENTER);
    }

    private void handleWindowClosing(boolean agent) {
        if (JOptionPane.showConfirmDialog(JByteMod.this, res.getResource("exit_warn"), res.getResource("is_sure"),
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (agent) {
                dispose();
            } else {
                discord.shutdown();
                Runtime.getRuntime().exit(0);
            }
        }
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        CommandLine cmd = parseCommandLine(args);
        if (cmd.hasOption("help")) {
            printHelpAndExit();
        }

        configureWorkingDirectory(cmd);
        configureConfigPath(cmd);
        initialize();

        EventQueue.invokeLater(() -> {
            try {
                initializeLookAndFeel();

                JByteMod frame = new JByteMod(false);
                instance = frame;
                frame.setVisible(true);

                loadFileIfNeeded(cmd, frame);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static CommandLine parseCommandLine(String[] args) {
        org.apache.commons.cli.Options options = buildCommandLineOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("An error occurred while parsing the commandline ");
        }
    }

    private static org.apache.commons.cli.Options buildCommandLineOptions() {
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        options.addOption("f", "file", true, "File to open");
        options.addOption("d", "dir", true, "Working directory");
        options.addOption("c", "config", true, "Config file name");
        options.addOption("?", "help", false, "Prints this help");
        return options;
    }

    private static void printHelpAndExit() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(jbytemod, buildCommandLineOptions());
        System.exit(0);
    }

    private static void configureWorkingDirectory(CommandLine cmd) {
        if (cmd.hasOption("d")) {
            workingDir = new File(cmd.getOptionValue("d"));
            if (!(workingDir.exists() && workingDir.isDirectory())) {
                printHelpAndExit();
            }
            JByteMod.LOGGER.err("Specified working dir set");
        }
    }

    private static void configureConfigPath(CommandLine cmd) {
        if (cmd.hasOption("c")) {
            configPath = cmd.getOptionValue("c");
        }
    }

    private static void initializeLookAndFeel() {
        if (!lafInit) {
            LookUtils.setTheme();
            lafInit = true;
        }
    }

    private static void loadFileIfNeeded(CommandLine cmd, JByteMod frame) {
        if (cmd.hasOption("f")) {
            File input = new File(cmd.getOptionValue("f"));
            if (FileUtils.exists(input) && FileUtils.isType(input, ".jar", ".class")) {
                frame.loadFile(input);
                JByteMod.LOGGER.log("Specified file loaded");
            } else {
                JByteMod.LOGGER.err("Specified file not found");
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
        new ErrorDisplay(new UnsupportedOperationException(res.getResource("jar_warn")));
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
        LOGGER.log("Building tree..");
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
        if (ops.get("select_code_tab").getBoolean()) {
            tabbedPane.setSelectedIndex(0);
        }
        this.currentNode = cn;
        this.currentMethod = null;
        sp.selectClass(cn);
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
            if (child.getMn() != null && child.getMn().equals(mn)) {
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
        if (ops.get("select_code_tab").getBoolean()) {
            tabbedPane.setSelectedIndex(0);
        }
        OpUtils.clearLabelCache();
        this.currentNode = cn;
        this.currentMethod = mn;
        sp.selectMethod(cn, mn);
        if (!codeList.loadInstructions(mn)) {
            codeList.setSelectedIndex(-1);
        }
        tcbList.addNodes(cn, mn);
        lvpList.addNodes(cn, mn);
        dp.setText("");
        tabbedPane.selectMethod(cn, mn);
        lastSelectedTreeEntries.put(cn, mn);
        if (lastSelectedTreeEntries.size() > 5) {
            lastSelectedTreeEntries.remove(lastSelectedTreeEntries.keySet().iterator().next());
        }
    }

    public void setDP(DecompilerPanel dp) {
        this.dp = dp;
    }

    public void setSP(InfoPanel sp) {
        this.sp = sp;
    }

    private void setTitleSuffix(String suffix) {
        this.setTitle(jbytemod + " - " + suffix);
    }

    @Override
    public void setVisible(boolean b) {
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
