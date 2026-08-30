package de.xbrowniecodez.jbytemod.ui;

import de.xbrowniecodez.jbytemod.plugin.Plugin;
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
import java.awt.Window;
import java.io.File;
import java.util.List;

public final class PluginManagerDialog extends JDialog {
    public PluginManagerDialog(Window owner, List<Plugin> plugins, File pluginFolder) {
        super(owner, "Manage Plugins", ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
        setContentPane(content);

        JLabel title = new JLabel("Loaded plugins");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1));
        content.add(title, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Name", "Version", "Author"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Plugin plugin : plugins) {
            model.addRow(new Object[]{plugin.getName(), plugin.getVersion(), plugin.getAuthor()});
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(1).setMinWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setMinWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton openFolder = new JButton("Open Plugins Folder");
        openFolder.addActionListener(event -> openPluginFolder(pluginFolder));
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
        setLocationRelativeTo(owner);
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
