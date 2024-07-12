package me.grax.jbytemod.ui;

import de.xbrowniecodez.jbytemod.Main;
import lombok.SneakyThrows;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

import java.awt.*;

public class DecompilerPanel extends RSyntaxTextArea {

    public DecompilerPanel() {
        this.setSyntaxEditingStyle("text/java");
        this.setCodeFoldingEnabled(true);
        this.setAntiAliasingEnabled(true);
        this.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.setEditable(false);
        this.setTheme();
    }

    @SneakyThrows
    public void setTheme() {
        Theme theme = Theme.load(Main.INSTANCE.getJByteMod().getOptions().get("use_dark_theme").getBoolean() ? getClass().getResourceAsStream("/resources/de/brownie/rsyntaxtextarea/themes/custom.xml") : getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
        theme.apply(this);
    }

}
