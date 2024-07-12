package me.grax.jbytemod.ui;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.utils.TextUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class JAboutFrame extends JDialog {

    public JAboutFrame(JByteMod jbm) {
        initializeUI(jbm);
    }

    private void initializeUI(JByteMod jbm) {
        setTitle(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("about") + " " + jbm.getTitle());
        setModal(true);
        setBounds(100, 100, 450, 300);
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setResizable(false);

        JButton closeButton = new JButton(Main.INSTANCE.getJByteMod().getLanguageRes().getResource("close"));
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4));
        for (int i = 0; i < 3; i++) {
            buttonPanel.add(new JPanel());
        }
        buttonPanel.add(closeButton);

        JTextPane titlePane = new JTextPane();
        titlePane.setContentType("text/html");
        titlePane.setText(TextUtils.toHtml(
                jbm.getTitle() +
                        "<br/>Copyright \u00A9 2016-2018 noverify<br/><br/>" +
                        "Copyright \u00A9 2019 Panda<br/><br/>" +
                        "Copyright \u00A9 2020-2024 xBrownieCodez<br/><br/>" +
                        "JByteMod by noverify, Reborn by Panda, Remastered by xBrownieCodez" +
                        "<br/><font color=\"#0000EE\"><u>https://github.com/xBrownieCodezV2/JByteMod-Remastered</u></font>"
        ));
        titlePane.setEditable(false);

        contentPanel.add(buttonPanel, BorderLayout.PAGE_END);
        contentPanel.add(titlePane, BorderLayout.CENTER);

        getContentPane().add(contentPanel);
    }
}
