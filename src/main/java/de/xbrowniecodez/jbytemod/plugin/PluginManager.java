package de.xbrowniecodez.jbytemod.plugin;

import lombok.Getter;
import me.grax.jbytemod.JByteMod;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
@Getter
public class PluginManager {

    private final ArrayList<Plugin> plugins = new ArrayList<>();
    private final File pluginFolder = new File(JByteMod.workingDir, "plugins");

    public PluginManager(JByteMod jbm) {
        if (pluginFolder.exists() && pluginFolder.isDirectory()) {
            loadPlugins();
        } else {
            JByteMod.LOGGER.err("No plugin folder found!");
        }
    }

    public static void addURL(URL u) {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(sysloader, u);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void loadPlugins() {
        File[] files = pluginFolder.listFiles();
        if (files == null) {
            JByteMod.LOGGER.err("Plugin folder is empty or does not exist!");
            return;
        }

        for (File file : files) {
            if (file.getName().endsWith(".jar")) {
                try (ZipFile zip = new ZipFile(file)) {
                    addURL(file.toURI().toURL());

                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();

                        if (name.endsWith(".class")) {
                            loadClassFromEntry(name);
                        }
                    }
                } catch (Exception e) {
                    JByteMod.LOGGER.err("Plugin " + file.getName() + " failed to load!");
                    e.printStackTrace();
                }
            }
        }
        JByteMod.LOGGER.log(plugins.size() + " plugin(s) loaded!");
    }

    private void loadClassFromEntry(String name) {
        try {
            String className = name.replace('/', '.').substring(0, name.length() - 6);
            Class<?> loadedClass = Class.forName(className, true, ClassLoader.getSystemClassLoader());

            if (Plugin.class.isAssignableFrom(loadedClass)) {
                Plugin pluginInstance = (Plugin) loadedClass.getDeclaredConstructor().newInstance();
                pluginInstance.init();
                this.plugins.add(pluginInstance);
            }
        } catch (Exception e) {
            JByteMod.LOGGER.err("Failed to load class " + name);
            e.printStackTrace();
        }
    }

}
