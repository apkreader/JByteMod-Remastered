package de.xbrowniecodez.jbytemod.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NotificationManager extends JFrame {
    private static final int NOTIFICATION_WIDTH = 300;
    private static final int NOTIFICATION_HEIGHT = 100;

    private static NotificationManager instance;

    private NotificationManager(String text) {
        initUI(text);
    }

    private void initUI(String text) {
        setUndecorated(true);
        setLayout(new BorderLayout());
        setSize(NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);
        setLocationRelativeTo(null);

        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        add(label, BorderLayout.CENTER);

        Timer timer = new Timer(3000, e -> dispose());
        timer.setRepeats(false);
        timer.start();
    }

    public static void showNotification(String text) {
        if (instance != null) {
            instance.dispose();
        }

        instance = new NotificationManager(text);
        instance.setVisible(true);
    }

}
