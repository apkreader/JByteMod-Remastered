package de.xbrowniecodez.jbytemod.utils.update.ui;

import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class UpdateDialogFrame extends JFrame {
    public UpdateDialogFrame(String latestVersion, String changelog) {
        JLabel label = new JLabel(String.format("Version %s is available, would you like to download it?", latestVersion));
        label.setBounds(10, 10, 400, 20);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(changelog);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBounds(10, 40, 570, 370);

        JButton yesButton = new JButton("Yes");
        yesButton.setBounds(10, 420, 80, 30);
        yesButton.addActionListener(e -> {
            openDownloadLink(latestVersion);
            dispose();
        });

        JButton noButton = new JButton("No");
        noButton.setBounds(100, 420, 80, 30);
        noButton.addActionListener(e -> dispose());

        add(label);
        add(scrollPane);
        add(yesButton);
        add(noButton);
        setLayout(null);
    }

    @SneakyThrows
    private void openDownloadLink(String version) {
        URI downloadUri = new URI(String.format(
                "https://github.com/apkreader/JByteMod-Remastered/releases/download/%s/JByteMod-Remastered-%s.jar",
                version, version));

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(downloadUri);
        }
    }

}
