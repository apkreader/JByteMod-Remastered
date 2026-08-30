package de.xbrowniecodez.jbytemod.ui;

import de.xbrowniecodez.jbytemod.JByteMod;
import de.xbrowniecodez.jbytemod.plugin.PluginManager;
import me.grax.jbytemod.utils.ErrorDisplay;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class PluginManagerDialog extends JDialog {
    public PluginManagerDialog(JByteMod jByteMod, PluginManager pluginManager) {
        super(jByteMod, "Manage Plugins", ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
        setContentPane(content);

        JLabel title = new JLabel("Loaded plugins");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1));
        content.add(title, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Enabled", "Name", "Version", "Author"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }
        };
        List<String> pluginIds = new ArrayList<>();
        for (PluginManager.PluginInfo plugin : pluginManager.getAvailablePlugins()) {
            pluginIds.add(plugin.id());
            model.addRow(new Object[]{plugin.enabled(), plugin.name(), plugin.version(), plugin.author()});
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(2).setMinWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setMinWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        model.addTableModelListener(event -> {
            if (event.getType() != javax.swing.event.TableModelEvent.UPDATE
                    || (event.getColumn() != 0 && event.getColumn() != javax.swing.event.TableModelEvent.ALL_COLUMNS)
                    || event.getFirstRow() < 0 || event.getFirstRow() >= pluginIds.size()) {
                return;
            }
            jByteMod.getPluginManager().setPluginEnabled(pluginIds.get(event.getFirstRow()),
                    (Boolean) model.getValueAt(event.getFirstRow(), 0));
            jByteMod.reloadPlugins();
        });

        JButton openFolder = new JButton("Open Folder");
        openFolder.addActionListener(event -> openPluginFolder(pluginManager.getPluginFolder()));
        JButton close = new JButton("Close");
        close.addActionListener(event -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(openFolder);
        buttons.add(close);
        content.add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(close);
        setMinimumSize(new Dimension(500, 260));
        setPreferredSize(new Dimension(580, 340));
        pack();
        setLocationRelativeTo(jByteMod);
    }

    private void openPluginFolder(File pluginFolder) {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new UnsupportedOperationException("Opening folders is not supported on this system");
            }
            Desktop.getDesktop().open(pluginFolder);
        } catch (Exception exception) {
            new ErrorDisplay(exception);
        }
    }
}
