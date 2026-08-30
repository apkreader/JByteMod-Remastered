package de.xbrowniecodez.jbytemod.plugin;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.Utils;
import lombok.Getter;
import de.xbrowniecodez.jbytemod.JByteMod;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Getter
public class PluginManager implements Closeable {

    private final ArrayList<Plugin> plugins = new ArrayList<>();
    private final File pluginFolder = new File(Utils.getWorkingDirectory(), "plugins");
    private final JByteMod jByteMod;
    private URLClassLoader classLoader;

    public PluginManager(JByteMod jbm) {
        this.jByteMod = jbm;
        if (!pluginFolder.exists() && !pluginFolder.mkdirs()) {
            Main.INSTANCE.getLogger().err("Could not create plugin folder: " + pluginFolder);
            return;
        }
        loadPlugins();
    }

    private void loadPlugins() {
        File[] files = pluginFolder.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
        if (files == null) {
            Main.INSTANCE.getLogger().err("Could not read plugin folder: " + pluginFolder);
            return;
        }

        java.util.Arrays.sort(files, Comparator.comparing(File::getName));
        URL[] pluginUrls = java.util.Arrays.stream(files).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (Exception exception) {
                throw new IllegalArgumentException(exception);
            }
        }).toArray(URL[]::new);
        classLoader = new URLClassLoader(pluginUrls, Plugin.class.getClassLoader());
        for (File file : files) {
            try (ZipFile zip = new ZipFile(file)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory() && name.endsWith(".class") && !name.equals("module-info.class")) {
                        loadClassFromEntry(classLoader, name);
                    }
                }
            } catch (Exception | LinkageError e) {
                Main.INSTANCE.getLogger().err("Plugin " + file.getName() + " failed to load!");
                e.printStackTrace();
            }
        }
        Main.INSTANCE.getLogger().log(plugins.size() + " plugin(s) loaded!");
    }

    private void loadClassFromEntry(URLClassLoader loader, String name) {
        try {
            String className = name.replace('/', '.').substring(0, name.length() - 6);
            Class<?> loadedClass = Class.forName(className, false, loader);

            if (loadedClass != Plugin.class && Plugin.class.isAssignableFrom(loadedClass)
                    && !Modifier.isAbstract(loadedClass.getModifiers())) {
                Plugin pluginInstance = (Plugin) loadedClass.getDeclaredConstructor().newInstance();
                pluginInstance.attach(jByteMod);
                pluginInstance.init();
                this.plugins.add(pluginInstance);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // A plugin jar may contain optional classes for another platform.
        } catch (ReflectiveOperationException | LinkageError e) {
            Main.INSTANCE.getLogger().err("Failed to load plugin class " + name);
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        for (Plugin plugin : plugins) {
            try {
                plugin.shutdown();
            } catch (Throwable throwable) {
                Main.INSTANCE.getLogger().err("Plugin " + plugin.getName() + " failed to shut down cleanly");
                throwable.printStackTrace();
            }
        }
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (Exception ignored) {
            }
        }
        classLoader = null;
        plugins.clear();
    }
}
