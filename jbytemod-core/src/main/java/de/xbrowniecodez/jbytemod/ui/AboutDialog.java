package de.xbrowniecodez.jbytemod.ui;

import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.utils.ErrorDisplay;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.URI;
import java.time.Year;

public final class AboutDialog extends JDialog {
    private static final URI PROJECT_URI = URI.create("https://github.com/jbytemod/JByteMod-Remastered");

    public AboutDialog(JByteMod jByteMod) {
        super(jByteMod, jByteMod.getLanguageRes().getResource("about") + " " + jByteMod.getTitle(),
                ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBorder(BorderFactory.createEmptyBorder(20, 22, 16, 22));
        setContentPane(content);

        JPanel information = new JPanel();
        information.setLayout(new BoxLayout(information, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(jByteMod.getTitle());
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 4));
        title.setAlignmentX(LEFT_ALIGNMENT);
        information.add(title);
        information.add(Box.createVerticalStrut(6));

        JLabel description = new JLabel("A Java bytecode editor and analysis tool.");
        description.setAlignmentX(LEFT_ALIGNMENT);
        information.add(description);
        information.add(Box.createVerticalStrut(18));

        information.add(credit("Original JByteMod", "loerting", "2016-2018"));
        information.add(Box.createVerticalStrut(5));
        information.add(credit("JByteMod Reborn", "Panda", "2019"));
        information.add(Box.createVerticalStrut(5));
        information.add(credit("JByteMod Remastered", "xBrownieCodez", "2020-" + Year.now().getValue()));
        information.add(Box.createVerticalStrut(13));

        JButton projectLink = new JButton("github.com/jbytemod/JByteMod-Remastered");
        projectLink.setAlignmentX(LEFT_ALIGNMENT);
        projectLink.setBorder(BorderFactory.createEmptyBorder());
        projectLink.setBorderPainted(false);
        projectLink.setContentAreaFilled(false);
        projectLink.setFocusPainted(false);
        projectLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        projectLink.setToolTipText(PROJECT_URI.toString());
        projectLink.addActionListener(event -> openProjectPage());
        information.add(projectLink);
        content.add(information, BorderLayout.CENTER);

        JButton close = new JButton(jByteMod.getLanguageRes().getResource("close"));
        close.addActionListener(event -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(close);
        content.add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(close);

        pack();
        setMinimumSize(new Dimension(470, getHeight()));
        setLocationRelativeTo(jByteMod);
    }

    private static JLabel credit(String project, String author, String years) {
        JLabel label = new JLabel(project + " - " + author + " (" + years + ")");
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private void openProjectPage() {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new UnsupportedOperationException("Opening links is not supported on this system");
            }
            Desktop.getDesktop().browse(PROJECT_URI);
        } catch (Exception exception) {
            new ErrorDisplay(exception);
        }
    }
}
