package me.grax.jbytemod.utils;

import de.xbrowniecodez.jbytemod.Main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorDisplay extends JFrame {
    private static final String suffix = "\n\nPlease report exceptions on github.\n\nhttps://github.com/GraxCode/JByteMod-Beta";

    public ErrorDisplay(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        _init_(t.getClass().getSimpleName(), sw.toString());
    }

    public ErrorDisplay(String s) {
        this("Error", s);
    }

    public ErrorDisplay(String title, String s) {
        _init_(title, s);
    }

    public static void error(String error) {
        JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void _init_(String title, String s) {
        this.setBounds(100, 100, 400, 600);
        this.setTitle(title);
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(5, 5));
        JPanel pageEnd = new JPanel();
        pageEnd.setLayout(new GridLayout(1, 6, 10, 10));

        contentPane.add(pageEnd, BorderLayout.PAGE_END);
        for (int i = 0; i < 4; i++) {
            pageEnd.add(new JPanel());
        }
        JButton close = new JButton(Main.INSTANCE.getJByteMod().getLanguageRes() != null ? Main.INSTANCE.getJByteMod().getLanguageRes().getResource("close") : "Close"); //res may not be loaded
        pageEnd.add(close);
        close.addActionListener(e -> ErrorDisplay.this.dispose());
        String st = s + suffix;
        contentPane.add(new JScrollPane(new JTextArea(st)), BorderLayout.CENTER);
        this.add(contentPane);
        this.setVisible(true);
    }
}
