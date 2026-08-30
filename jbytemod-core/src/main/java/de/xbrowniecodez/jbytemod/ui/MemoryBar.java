package de.xbrowniecodez.jbytemod.ui;

import javax.swing.*;
import java.awt.*;

public class MemoryBar extends JPanel {
    private JProgressBar progressBar;

    public MemoryBar() {
        setLayout(new BorderLayout());
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        updateMemoryUsage();

        Timer timer = new Timer(1000, e -> updateMemoryUsage());
        timer.start();

        add(progressBar, BorderLayout.CENTER);
    }

    private void updateMemoryUsage() {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        int progressBarMaxValue = progressBar.getMaximum();
        int scaledValue = (int) ((double) usedMemory / totalMemory * progressBarMaxValue);

        progressBar.setValue(scaledValue);
        progressBar.setString("Used Memory: " + formatMemorySize(usedMemory) + "/" + formatMemorySize(totalMemory));
    }

    private String formatMemorySize(long bytes) {
        long kilobytes = bytes / 1024;
        long megabytes = kilobytes / 1024;
        long gigabytes = megabytes / 1024;

        if (gigabytes > 0) {
            return gigabytes + " GB";
        } else if (megabytes > 0) {
            return megabytes + " MB";
        } else {
            return kilobytes + " KB";
        }
    }
}
