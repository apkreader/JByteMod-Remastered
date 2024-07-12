package me.grax.jbytemod.utils.gui;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import com.github.weisj.darklaf.theme.OneDarkTheme;
import me.grax.jbytemod.JByteMod;
import me.grax.jbytemod.ui.lists.MyCodeList;

public class LookUtils {

    public static void setTheme() {
        JByteMod.LOGGER.log("Setting Theme");
        LafManager.install(JByteMod.ops.get("use_dark_theme").getBoolean() ? new OneDarkTheme() : new IntelliJTheme());
    }
    public static void changeTheme() {
        if(JByteMod.ops.get("use_dark_theme").getBoolean()) {
            LafManager.setTheme(new OneDarkTheme());
        } else {
            LafManager.setTheme(new IntelliJTheme());
        }
        LafManager.install();
        JByteMod.instance.initializeFrame(false);
        JByteMod.instance.getDp().setTheme();
    }
}
