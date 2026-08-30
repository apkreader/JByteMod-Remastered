package de.xbrowniecodez.jbytemod.utils;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.os.OSUtil;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

@UtilityClass
public class Utils {
    private static Properties cachedProperties;

    public Properties readPropertiesFile() {
        if (cachedProperties == null) {
            try (InputStream stream = Utils.class.getResourceAsStream("/resources/jbytemod.properties")) {
                Properties prop = new Properties();
                prop.load(stream);
                cachedProperties = prop;
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        return cachedProperties;
    }

    public File getWorkingDirectory() {
        String userHome = System.getProperty("user.home", ".");
        String jbytePath = "JByteMod-Remastered/";
        File workingDirectory;
        switch (Objects.requireNonNull(OSUtil.getCurrentOS())) {
            case WINDOWS:
                String applicationData = System.getenv("APPDATA");
                String folder = applicationData != null ? applicationData : userHome;
                workingDirectory = new File(folder, jbytePath);
                break;
            case MAC:
                workingDirectory = new File(userHome, "Library/Application Support/" + jbytePath);
                break;
            default:
                workingDirectory = new File(userHome, jbytePath);
                break;
        }

        if (!workingDirectory.exists()) {
            if (!workingDirectory.mkdir()) {
                 Main.INSTANCE.getLogger().err("Failed to create working directory!");
                return new File(".");
            }
        }
         Main.INSTANCE.getLogger().log("Working directory " + workingDirectory);
        return workingDirectory;
    }
}
