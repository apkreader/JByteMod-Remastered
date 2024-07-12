package me.grax.jbytemod.utils;

import de.xbrowniecodez.jbytemod.Main;

public class TextUtils {

    public static String toHtml(String str) {
        return "<html>" + toThemeColor(str);
    }

    public static String toThemeColor(String str) {
        String fontColor = Main.INSTANCE.getJByteMod().getOptions().get("use_dark_theme").getBoolean() ? "#aba9a9" : "#000000";
        return addTag(str, "font color=" + fontColor);
    }

    public static String addTag(String str, String tag) {
        return "<" + tag + ">" + str + "</" + tag.split(" ")[0] + ">";
    }

    public static String toLight(String str) {
        return addTag(str, "font color=#999999");
    }

    public static String toBold(String str) {
        return addTag(str, "b");
    }

    public static String escape(String str) {
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String toItalics(String str) {
        return addTag(str, "i");
    }

    public static String max(String string, int i) {
        if (string.length() > i) {
            return string.substring(0, i) + "...";
        }
        return string;
    }
}
