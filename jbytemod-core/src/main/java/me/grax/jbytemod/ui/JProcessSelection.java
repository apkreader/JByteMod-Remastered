package me.grax.jbytemod.ui;

import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

public class JProcessSelection extends JDialog {

    protected int pid;
    private JPanel contentPane;
    private JButton btn;

    public JProcessSelection(List<VirtualMachineDescriptor> list) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        setTitle("Select process");
        setModalityType(ModalityType.APPLICATION_MODAL);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);
        String[][] entries = new String[list.size()][2];
        int i = 0;
        for (VirtualMachineDescriptor vmd : list) {
            entries[i++] = new String[]{vmd.displayName(), vmd.id()};
        }
        JTable table = new JTable(entries, new Object[]{"Display Name", "PID"});
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        table.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                btn.setEnabled(table.getSelectedRow() != -1);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        JPanel bpanel = new JPanel(new GridLayout(1, 6));
        bpanel.setEnabled(false);
        btn = new JButton("Select");
        btn.setEnabled(false);
        btn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int pid = Integer.parseInt(table.getValueAt(table.getSelectedRow(), 1).toString().trim());
                JProcessSelection.this.pid = pid;
                dispose();
            }
        });
        for (i = 0; i < 5; i++) {
            bpanel.add(new JPanel());
        }
        bpanel.add(btn);
        contentPane.add(bpanel, BorderLayout.SOUTH);

    }

    public int getPid() {
        return pid;
    }

}
