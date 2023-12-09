package me.grax.jbytemod.ui;

import me.grax.jbytemod.JByteMod;
import me.grax.jbytemod.utils.ErrorDisplay;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

import java.awt.*;
import java.io.IOException;

public class DecompilerPanel extends RSyntaxTextArea {

    public DecompilerPanel() {
        this.setSyntaxEditingStyle("text/java");
        this.setCodeFoldingEnabled(true);
        this.setAntiAliasingEnabled(true);
        this.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.setEditable(false);
        // change theme to java
        try {
            Theme theme;
            if(JByteMod.ops.get("use_dark_theme").getBoolean())
                theme = Theme.load(getClass().getResourceAsStream("/resources/de/brownie/rsyntaxtextarea/themes/custom.xml"));
            else
                theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
            theme.apply(this);
        } catch (IOException e1) {
            new ErrorDisplay(e1);
        }
    }
}
