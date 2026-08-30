package de.xbrowniecodez.jbytemod.utils.gui;

import com.github.weisj.darklaf.DarkLaf;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import com.github.weisj.darklaf.theme.OneDarkTheme;
import de.xbrowniecodez.jbytemod.Main;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;

public class LookUtils {

    public static void setTheme() {
        Main.INSTANCE.getLogger().log("Setting Theme");
        LafManager.install(Main.INSTANCE.getJByteMod().getOptions().get("use_dark_theme").getBoolean() ? new OneDarkTheme() : new IntelliJTheme());
    }

    public static void changeTheme() {
        if (Main.INSTANCE.getJByteMod().isAgent()
                && Main.INSTANCE.getJByteMod().getAgentInstrumentation() != null) {
            applyAgentTheme(Main.INSTANCE.getJByteMod());
            Main.INSTANCE.getJByteMod().getDecompilerPanel().setTheme();
            return;
        }

        if(Main.INSTANCE.getJByteMod().getOptions().get("use_dark_theme").getBoolean()) {
            LafManager.setTheme(new OneDarkTheme());
        } else {
            LafManager.setTheme(new IntelliJTheme());
        }
        LafManager.install();
        Main.INSTANCE.getJByteMod().getDecompilerPanel().setTheme();
    }

    public static void applyAgentTheme(Component root) {
        applyAgentTheme(root, null);
    }

    public static void applyAgentTheme(Component root, Runnable afterApply) {
        LookAndFeel previous = UIManager.getLookAndFeel();
        boolean applied = false;
        try {
            LafManager.setTheme(Main.INSTANCE.getJByteMod().getOptions().get("use_dark_theme").getBoolean()
                    ? new OneDarkTheme()
                    : new IntelliJTheme());
            UIManager.setLookAndFeel(new DarkLaf());
            SwingUtilities.updateComponentTreeUI(root);
            applied = true;
        } catch (Exception e) {
            Main.INSTANCE.getLogger().err("Failed to apply theme: " + e.getMessage());
        }

        try {
            if (afterApply != null) {
                afterApply.run();
            }
        } finally {
            if (applied && previous != null) {
                try {
                    UIManager.setLookAndFeel(previous);
                } catch (Exception e) {
                    Main.INSTANCE.getLogger().err("Failed to restore target theme: " + e.getMessage());
                }
            }
        }
    }
}
