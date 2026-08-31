package de.xbrowniecodez.jbytemod.ui;

import de.xbrowniecodez.jbytemod.JByteMod;
import de.xbrowniecodez.jbytemod.plugin.PluginManager;
import de.xbrowniecodez.jbytemod.plugin.PluginRepositoryService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public final class PluginManagerDialog extends JDialog {
    private final JByteMod jByteMod;
    private final PluginRepositoryService repositoryService = new PluginRepositoryService();
    private final DefaultTableModel installedModel;
    private final DefaultTableModel repositoryModel;
    private final List<String> installedPluginIds = new ArrayList<>();
    private final List<PluginRepositoryService.RepositoryPlugin> catalog = new ArrayList<>();
    private final List<PluginRepositoryService.RepositoryPlugin> visibleCatalog = new ArrayList<>();
    private final JTable repositoryTable;
    private final JTextArea description = new JTextArea();
    private final JTextField search = new JTextField();
    private final JLabel repositoryStatus = new JLabel(" ");
    private final JLabel selectedPluginName = new JLabel("Select a plugin");
    private final JLabel selectedPluginMetadata = new JLabel(" ");
    private final JButton install = new JButton("Install");
    private final JButton website = new JButton("Open Page");
    private final JButton refresh = new JButton("Refresh");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private boolean updatingInstalledModel;

    public PluginManagerDialog(JByteMod jByteMod, PluginManager pluginManager) {
        super(jByteMod, "Manage Plugins", ModalityType.MODELESS);
        this.jByteMod = jByteMod;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        installedModel = new DefaultTableModel(new Object[]{"Enabled", "Name", "Version", "Author"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }
        };
        repositoryModel = new DefaultTableModel(
                new Object[]{"Name", "Version", "Author", "Repository", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        repositoryTable = new JTable(repositoryModel);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(content);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Installed", createInstalledPanel());
        tabs.addTab("Plugin Repository", createRepositoryPanel());
        content.add(tabs, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(event -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.add(close);
        content.add(bottom, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(close);

        refreshInstalledPlugins(pluginManager);
        loadRepositories();
        setMinimumSize(new Dimension(700, 420));
        setPreferredSize(new Dimension(820, 500));
        pack();
        setLocationRelativeTo(jByteMod);
    }

    private JPanel createInstalledPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        JTable table = new JTable(installedModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(table.getRowHeight() + 4);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        installedModel.addTableModelListener(event -> {
            if (updatingInstalledModel || event.getType() != TableModelEvent.UPDATE
                    || event.getColumn() != 0 || event.getFirstRow() < 0
                    || event.getFirstRow() >= installedPluginIds.size()) {
                return;
            }
            String pluginId = installedPluginIds.get(event.getFirstRow());
            boolean enabled = Boolean.TRUE.equals(installedModel.getValueAt(event.getFirstRow(), 0));
            jByteMod.getPluginManager().setPluginEnabled(pluginId, enabled);
            jByteMod.reloadPlugins();
            refreshInstalledPlugins(jByteMod.getPluginManager());
            applyCatalogFilter();
        });

        JButton openFolder = new JButton("Open Plugin Folder");
        openFolder.addActionListener(event -> openPluginFolder(jByteMod.getPluginManager().getPluginFolder()));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(openFolder);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRepositoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));

        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyCatalogFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyCatalogFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyCatalogFilter();
            }
        });
        JPanel searchPanel = new JPanel(new BorderLayout(7, 0));
        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        searchPanel.add(search, BorderLayout.CENTER);
        toolbar.add(searchPanel, BorderLayout.CENTER);
        JButton repositories = new JButton("Repositories...");
        repositories.addActionListener(event -> showRepositoriesDialog());
        refresh.addActionListener(event -> loadRepositories());
        JPanel toolbarButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        toolbarButtons.add(repositories);
        toolbarButtons.add(refresh);
        toolbar.add(toolbarButtons, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);

        repositoryTable.setFillsViewportHeight(true);
        repositoryTable.setRowHeight(repositoryTable.getRowHeight() + 4);
        repositoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repositoryTable.setAutoCreateRowSorter(true);
        repositoryTable.getTableHeader().setReorderingAllowed(false);
        repositoryTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        repositoryTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        repositoryTable.getColumnModel().getColumn(3).setPreferredWidth(105);
        repositoryTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        repositoryTable.getSelectionModel().addListSelectionListener(event -> updateRepositorySelection());

        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setRows(2);
        description.setOpaque(false);
        description.setFocusable(false);
        description.setBorder(null);
        panel.add(new JScrollPane(repositoryTable), BorderLayout.CENTER);

        website.setEnabled(false);
        website.addActionListener(event -> openSelectedWebsite());
        install.setEnabled(false);
        install.addActionListener(event -> installSelectedPlugin());
        progress.setVisible(false);
        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(120, progress.getPreferredSize().height));
        JPanel details = new JPanel(new BorderLayout(10, 7));
        Color separator = UIManager.getColor("Separator.foreground");
        if (separator == null) separator = UIManager.getColor("Component.borderColor");
        if (separator == null) separator = Color.GRAY;
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, separator),
                BorderFactory.createEmptyBorder(10, 4, 0, 4)));

        selectedPluginName.setFont(selectedPluginName.getFont().deriveFont(Font.BOLD,
                selectedPluginName.getFont().getSize2D() + 1));
        Color secondary = UIManager.getColor("Label.disabledForeground");
        if (secondary != null) selectedPluginMetadata.setForeground(secondary);
        JPanel selectedHeader = new JPanel(new BorderLayout(0, 3));
        selectedHeader.add(selectedPluginName, BorderLayout.NORTH);
        selectedHeader.add(selectedPluginMetadata, BorderLayout.CENTER);
        details.add(selectedHeader, BorderLayout.NORTH);
        details.add(description, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout(8, 0));
        repositoryStatus.setFont(repositoryStatus.getFont().deriveFont(repositoryStatus.getFont().getSize2D() - 1));
        actions.add(repositoryStatus, BorderLayout.CENTER);
        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionButtons.add(progress);
        actionButtons.add(website);
        actionButtons.add(install);
        actions.add(actionButtons, BorderLayout.EAST);
        details.add(actions, BorderLayout.SOUTH);
        panel.add(details, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshInstalledPlugins(PluginManager manager) {
        updatingInstalledModel = true;
        try {
            installedModel.setRowCount(0);
            installedPluginIds.clear();
            manager.getAvailablePlugins().stream()
                    .sorted(Comparator.comparing(PluginManager.PluginInfo::name, String.CASE_INSENSITIVE_ORDER))
                    .forEach(plugin -> {
                        installedPluginIds.add(plugin.id());
                        installedModel.addRow(new Object[]{plugin.enabled(), plugin.name(), plugin.version(), plugin.author()});
                    });
        } finally {
            updatingInstalledModel = false;
        }
    }

    private void loadRepositories() {
        refresh.setEnabled(false);
        install.setEnabled(false);
        repositoryStatus.setText("Loading plugin repositories...");
        new SwingWorker<PluginRepositoryService.Catalog, Void>() {
            @Override
            protected PluginRepositoryService.Catalog doInBackground() {
                return repositoryService.loadCatalog();
            }

            @Override
            protected void done() {
                refresh.setEnabled(true);
                try {
                    PluginRepositoryService.Catalog result = get();
                    catalog.clear();
                    catalog.addAll(result.plugins());
                    applyCatalogFilter();
                    if (result.messages().isEmpty()) {
                        repositoryStatus.setText(catalog.size() + " plugin" + (catalog.size() == 1 ? "" : "s") + " available");
                        repositoryStatus.setToolTipText(null);
                    } else {
                        repositoryStatus.setText(catalog.size() + " plugins available; some repositories reported a problem");
                        repositoryStatus.setToolTipText(String.join(" | ", result.messages()));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    showError("Could not load plugin repositories", exception.getCause());
                    repositoryStatus.setText("Could not load plugin repositories");
                }
            }
        }.execute();
    }

    private void applyCatalogFilter() {
        PluginRepositoryService.RepositoryPlugin selected = selectedRepositoryPlugin();
        String selectedId = selected == null ? null : selected.id();
        String filter = search.getText().trim().toLowerCase(Locale.ROOT);
        visibleCatalog.clear();
        repositoryModel.setRowCount(0);
        for (PluginRepositoryService.RepositoryPlugin plugin : catalog) {
            String searchable = String.join(" ", plugin.name(), plugin.author(), plugin.description(),
                    plugin.repositoryName()).toLowerCase(Locale.ROOT);
            if (!filter.isEmpty() && !searchable.contains(filter)) continue;
            visibleCatalog.add(plugin);
            repositoryModel.addRow(new Object[]{plugin.name(), plugin.version(), plugin.author(),
                    repositoryDisplayName(plugin), pluginStatus(plugin)});
        }
        if (visibleCatalog.isEmpty()) {
            updateRepositorySelection();
            return;
        }
        int modelRow = 0;
        if (selectedId != null) {
            for (int index = 0; index < visibleCatalog.size(); index++) {
                if (visibleCatalog.get(index).id().equals(selectedId)) {
                    modelRow = index;
                    break;
                }
            }
        }
        int selectedModelRow = modelRow;
        SwingUtilities.invokeLater(() -> {
            int viewRow = repositoryTable.convertRowIndexToView(selectedModelRow);
            if (viewRow >= 0) repositoryTable.setRowSelectionInterval(viewRow, viewRow);
        });
    }

    private String repositoryDisplayName(PluginRepositoryService.RepositoryPlugin plugin) {
        if (plugin.repositoryUrl().equalsIgnoreCase(PluginRepositoryService.OFFICIAL_REPOSITORY)) {
            return "Official";
        }
        return plugin.repositoryName();
    }

    private String pluginStatus(PluginRepositoryService.RepositoryPlugin plugin) {
        if (compareVersions(plugin.minimumJByteModVersion(), jByteMod.getVersion().toString()) > 0) {
            return "Requires JByteMod " + plugin.minimumJByteModVersion();
        }
        PluginManager.PluginInfo installedPlugin = installedPlugin(plugin.id());
        if (!plugin.downloadable()) return "No verified release";
        if (installedPlugin == null) return "Available";
        if (compareVersions(plugin.version(), installedPlugin.version()) > 0) return "Update available";
        return "Installed";
    }

    private PluginManager.PluginInfo installedPlugin(String id) {
        return jByteMod.getPluginManager().getAvailablePlugins().stream()
                .filter(plugin -> plugin.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private void updateRepositorySelection() {
        PluginRepositoryService.RepositoryPlugin plugin = selectedRepositoryPlugin();
        if (plugin == null) {
            selectedPluginName.setText("Select a plugin");
            selectedPluginMetadata.setText("Choose an entry above to view its details.");
            description.setText("");
            website.setEnabled(false);
            install.setEnabled(false);
            return;
        }
        selectedPluginName.setText(plugin.name());
        selectedPluginMetadata.setText("Version " + plugin.version() + "  |  " + plugin.author() + "  |  "
                + repositoryDisplayName(plugin) + " repository");
        description.setText(plugin.description());
        description.setCaretPosition(0);
        website.setEnabled(!plugin.website().isBlank());
        boolean compatible = compareVersions(plugin.minimumJByteModVersion(), jByteMod.getVersion().toString()) <= 0;
        install.setEnabled(plugin.downloadable() && compatible);
        PluginManager.PluginInfo installedPlugin = installedPlugin(plugin.id());
        if (installedPlugin == null) install.setText("Install");
        else if (compareVersions(plugin.version(), installedPlugin.version()) > 0) install.setText("Update");
        else install.setText("Reinstall");
    }

    private PluginRepositoryService.RepositoryPlugin selectedRepositoryPlugin() {
        int row = repositoryTable.getSelectedRow();
        if (row < 0) return null;
        int modelRow = repositoryTable.convertRowIndexToModel(row);
        return modelRow >= 0 && modelRow < visibleCatalog.size() ? visibleCatalog.get(modelRow) : null;
    }

    private void installSelectedPlugin() {
        PluginRepositoryService.RepositoryPlugin plugin = selectedRepositoryPlugin();
        if (plugin == null) return;
        setRepositoryBusy(true);
        repositoryStatus.setText("Downloading " + plugin.name() + "...");
        new SwingWorker<Path, Integer>() {
            @Override
            protected Path doInBackground() throws Exception {
                return repositoryService.downloadPlugin(plugin, value -> publish(value));
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) progress.setValue(chunks.getLast());
            }

            @Override
            protected void done() {
                Path downloaded = null;
                try {
                    downloaded = get();
                    PluginManager manager = jByteMod.getPluginManager();
                    try {
                        manager.installPluginJar(plugin, downloaded);
                    } finally {
                        jByteMod.reloadPlugins();
                    }
                    refreshInstalledPlugins(jByteMod.getPluginManager());
                    applyCatalogFilter();
                    repositoryStatus.setText(plugin.name() + " " + plugin.version() + " installed");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    showError("Could not install " + plugin.name(), exception.getCause());
                    repositoryStatus.setText("Installation failed");
                } catch (Exception exception) {
                    showError("Could not install " + plugin.name(), exception);
                    repositoryStatus.setText("Installation failed");
                } finally {
                    if (downloaded != null) {
                        try {
                            Files.deleteIfExists(downloaded);
                        } catch (IOException ignored) {
                        }
                    }
                    setRepositoryBusy(false);
                    updateRepositorySelection();
                }
            }
        }.execute();
    }

    private void setRepositoryBusy(boolean busy) {
        progress.setValue(0);
        progress.setVisible(busy);
        refresh.setEnabled(!busy);
        search.setEnabled(!busy);
        repositoryTable.setEnabled(!busy);
        install.setEnabled(!busy && selectedRepositoryPlugin() != null);
    }

    private void showRepositoriesDialog() {
        JDialog dialog = new JDialog(this, "Plugin Repositories", ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        dialog.setContentPane(content);

        DefaultListModel<PluginRepositoryService.RepositorySource> model = new DefaultListModel<>();
        repositoryService.getSources().forEach(model::addElement);
        JList<PluginRepositoryService.RepositorySource> sources = new JList<>(model);
        sources.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sources.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                PluginRepositoryService.RepositorySource source = (PluginRepositoryService.RepositorySource) value;
                label.setText(source.official() ? "Official JByteMod repository (built in)" : source.url());
                label.setToolTipText(source.url());
                return label;
            }
        });
        content.add(new JScrollPane(sources), BorderLayout.CENTER);

        JButton add = new JButton("Add...");
        add.addActionListener(event -> {
            String value = JOptionPane.showInputDialog(dialog,
                    "Enter a GitHub repository URL or a direct plugins.json URL:", "Add Plugin Repository",
                    JOptionPane.PLAIN_MESSAGE);
            if (value == null) return;
            try {
                repositoryService.addSource(value);
                model.clear();
                repositoryService.getSources().forEach(model::addElement);
            } catch (RuntimeException exception) {
                showError("Could not add repository", exception);
            }
        });
        JButton remove = new JButton("Remove");
        remove.setEnabled(false);
        sources.addListSelectionListener(event -> {
            PluginRepositoryService.RepositorySource selected = sources.getSelectedValue();
            remove.setEnabled(selected != null && !selected.official());
        });
        remove.addActionListener(event -> {
            PluginRepositoryService.RepositorySource selected = sources.getSelectedValue();
            if (selected == null || selected.official()) return;
            repositoryService.removeSource(selected);
            model.removeElement(selected);
        });
        JButton close = new JButton("Close");
        close.addActionListener(event -> dialog.dispose());
        JPanel buttons = new JPanel(new GridLayout(1, 0, 6, 0));
        buttons.add(add);
        buttons.add(remove);
        buttons.add(close);
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonContainer.add(buttons);
        content.add(buttonContainer, BorderLayout.SOUTH);

        dialog.getRootPane().setDefaultButton(close);
        dialog.setMinimumSize(new Dimension(560, 260));
        dialog.setPreferredSize(new Dimension(660, 320));
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        loadRepositories();
    }

    private void openSelectedWebsite() {
        PluginRepositoryService.RepositoryPlugin plugin = selectedRepositoryPlugin();
        if (plugin == null || plugin.website().isBlank()) return;
        try {
            Desktop.getDesktop().browse(URI.create(plugin.website()));
        } catch (Exception exception) {
            showError("Could not open plugin page", exception);
        }
    }

    private void openPluginFolder(File pluginFolder) {
        try {
            if (!Desktop.isDesktopSupported()) {
                JFileChooser chooser = new JFileChooser(pluginFolder);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.showOpenDialog(this);
                return;
            }
            Desktop.getDesktop().open(pluginFolder);
        } catch (Exception exception) {
            showError("Could not open plugin folder", exception);
        }
    }

    private int compareVersions(String left, String right) {
        int[] leftParts = versionParts(left);
        int[] rightParts = versionParts(right);
        for (int index = 0; index < Math.max(leftParts.length, rightParts.length); index++) {
            int leftPart = index < leftParts.length ? leftParts[index] : 0;
            int rightPart = index < rightParts.length ? rightParts[index] : 0;
            if (leftPart != rightPart) return Integer.compare(leftPart, rightPart);
        }
        return 0;
    }

    private int[] versionParts(String version) {
        String[] parts = version.split("[^0-9]+");
        List<Integer> numbers = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) numbers.add(Integer.parseInt(part));
        }
        return numbers.stream().mapToInt(Integer::intValue).toArray();
    }

    private void showError(String title, Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getMessage() == null) cause = cause.getCause();
        String message = cause.getMessage() == null ? cause.toString() : cause.getMessage();
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
