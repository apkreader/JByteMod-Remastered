package me.grax.jbytemod.ui;

import de.xbrowniecodez.jbytemod.ui.MemoryBar;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panel displayed at the bottom of the page.
 */
public class PageEndPanel extends JPanel {

    private static final String COPYRIGHT_TEXT = "\u00A9 brownie 2020 - 2023";
    private JProgressBar progressBar;
    private JLabel percentLabel;
    private JLabel copyrightLabel;
    private MemoryBar memoryBar;

    public PageEndPanel() {
        progressBar = new JProgressBar() {
            @Override
            public void setValue(int n) {
                if (n == 100) {
                    super.setValue(0);
                    percentLabel.setText("");
                } else {
                    super.setValue(n);
                    percentLabel.setText(n + "%");
                }
            }
        };

        setLayout(new GridLayout(1, 4, 10, 10));
        setBorder(new EmptyBorder(3, 0, 0, 0));

        add(progressBar);
        add(percentLabel = new JLabel());
        percentLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        copyrightLabel = new JLabel(COPYRIGHT_TEXT);
        copyrightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        copyrightLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        add(copyrightLabel);

        memoryBar = new MemoryBar();
        add(memoryBar);
    }

    public void setValue(int n) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(n);
            progressBar.repaint();
        });
    }

    public void setTip(String tooltipText) {
        if (tooltipText != null) {
            copyrightLabel.setText(tooltipText);
        } else {
            copyrightLabel.setText(COPYRIGHT_TEXT);
        }
    }
}
