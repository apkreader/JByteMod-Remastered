package de.xbrowniecodez.jbytemod.ui;

import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public final class JProcessSelection extends JDialog {
    private int pid;

    public JProcessSelection(Window owner, List<VirtualMachineDescriptor> processes) {
        super(owner, "Attach to JVM", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
        setContentPane(content);

        JPanel heading = new JPanel(new BorderLayout(0, 4));
        JLabel title = new JLabel("Select a running JVM");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1));
        heading.add(title, BorderLayout.NORTH);
        JLabel description = new JLabel("Choose the process whose loaded classes you want to inspect and modify.");
        description.setEnabled(false);
        heading.add(description, BorderLayout.CENTER);
        content.add(heading, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Process", "PID"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (VirtualMachineDescriptor process : processes) {
            String displayName = process.displayName().isBlank() ? "Unnamed JVM" : process.displayName();
            model.addRow(new Object[]{displayName, process.id()});
        }

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(Math.max(table.getRowHeight(), 24));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(1).setMinWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setMaxWidth(110);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton attach = new JButton("Attach");
        attach.setEnabled(false);
        attach.addActionListener(event -> select(table));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(event -> dispose());

        table.getSelectionModel().addListSelectionListener(event ->
                attach.setEnabled(!event.getValueIsAdjusting() && table.getSelectedRow() >= 0));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    select(table);
                }
            }
        });
        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            attach.setEnabled(true);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancel);
        buttons.add(attach);
        content.add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(attach);
        getRootPane().registerKeyboardAction(event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setMinimumSize(new Dimension(520, 320));
        setPreferredSize(new Dimension(620, 380));
        pack();
        setLocationRelativeTo(owner);
    }

    private void select(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        pid = Integer.parseInt(table.getModel().getValueAt(modelRow, 1).toString());
        dispose();
    }

    public int getPid() {
        return pid;
    }
}
