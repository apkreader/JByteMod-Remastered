package de.xbrowniecodez.jbytemod.utils.os;


public class OSUtil {
    private static final String osName = System.getProperty("os.name").toLowerCase();

    public static OperatingSystem getCurrentOS() {
        if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (osName.contains("mac")) {
            return OperatingSystem.MAC;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return OperatingSystem.UNIX;
        } else {
            return null;
        }
    }
}
