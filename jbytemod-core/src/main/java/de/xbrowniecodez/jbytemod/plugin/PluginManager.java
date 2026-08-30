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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@Getter
public class PluginManager implements Closeable {

    private final ArrayList<Plugin> plugins = new ArrayList<>();
    private final ArrayList<PluginInfo> availablePlugins = new ArrayList<>();
    private final File pluginFolder = new File(Utils.getWorkingDirectory(), "plugins");
    private final JByteMod jByteMod;
    private final PluginContext pluginContext;
    private URLClassLoader classLoader;
    private final Preferences preferences = Preferences.userNodeForPackage(PluginManager.class);

    private static final String FILE_ENABLED_KEY_PREFIX = "fileEnabled.";
    private static final String PLUGIN_ENABLED_KEY_PREFIX = "pluginEnabled.";
    private static final String INFO_KEY_PREFIX = "info.";
    private static final String FILE_PLUGIN_IDS_KEY_PREFIX = "filePlugins.";
    private static final String INFO_SEPARATOR = "\u001f";

    public record PluginInfo(String id, String name, String version, String author, boolean enabled,
                             String sourceFile) {
    }

    public PluginManager(JByteMod jbm) {
        this.jByteMod = jbm;
        this.pluginContext = new JByteModPluginContext(jbm);
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
        URL[] pluginUrls = java.util.Arrays.stream(files).filter(this::isFileEnabled).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (Exception exception) {
                throw new IllegalArgumentException(exception);
            }
        }).toArray(URL[]::new);
        classLoader = new URLClassLoader(pluginUrls, Plugin.class.getClassLoader());
        for (File file : files) {
            if (!isFileEnabled(file)) {
                addCachedPlugins(file);
                continue;
            }
            try (ZipFile zip = new ZipFile(file)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory() && name.endsWith(".class")
                            && !name.startsWith("META-INF/") && !name.equals("module-info.class")) {
                        loadClassFromEntry(classLoader, name, file);
                    }
                }
            } catch (Exception | LinkageError e) {
                Main.INSTANCE.getLogger().err("Plugin " + file.getName() + " failed to load!");
                e.printStackTrace();
            }
        }
        Main.INSTANCE.getLogger().log(plugins.size() + " plugin(s) loaded!");
    }

    private void loadClassFromEntry(URLClassLoader loader, String name, File sourceFile) {
        try {
            String className = name.replace('/', '.').substring(0, name.length() - 6);
            Class<?> loadedClass = Class.forName(className, false, loader);

            if (loadedClass != Plugin.class && Plugin.class.isAssignableFrom(loadedClass)
                    && !Modifier.isAbstract(loadedClass.getModifiers())) {
                Plugin pluginInstance = (Plugin) loadedClass.getDeclaredConstructor().newInstance();
                PluginInfo pluginInfo = new PluginInfo(className, pluginInstance.getName(), pluginInstance.getVersion(),
                        pluginInstance.getAuthor(), true, sourceFile.getName());
                availablePlugins.add(pluginInfo);
                cachePluginInfo(pluginInfo);
                pluginInstance.attach(pluginContext);
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

    public void setPluginEnabled(String id, boolean enabled) {
        for (PluginInfo plugin : availablePlugins) {
            if (plugin.id().equals(id)) {
                cachePluginInfo(plugin);
                preferences.putBoolean(PLUGIN_ENABLED_KEY_PREFIX + plugin.id(), enabled);
                preferences.putBoolean(FILE_ENABLED_KEY_PREFIX + plugin.sourceFile(), enabled);
                flushPreferences();
                break;
            }
        }
    }

    private void flushPreferences() {
        try {
            preferences.flush();
        } catch (BackingStoreException exception) {
            Main.INSTANCE.getLogger().err("Could not save plugin settings");
            exception.printStackTrace();
        }
    }

    private boolean isFileEnabled(File file) {
        if (!preferences.getBoolean(FILE_ENABLED_KEY_PREFIX + file.getName(), true)) {
            return false;
        }
        for (String className : getClassNames(file)) {
            if (!isPluginEnabled(className)) {
                return false;
            }
        }
        return true;
    }

    private void addCachedPlugins(File file) {
        Set<String> ids = new LinkedHashSet<>();
        for (String id : preferences.get(FILE_PLUGIN_IDS_KEY_PREFIX + file.getName(), "").split("\\|")) {
            if (!id.isEmpty()) ids.add(id);
        }
        for (String className : getClassNames(file)) {
            if (preferences.get(INFO_KEY_PREFIX + className, null) != null) {
                ids.add(className);
            }
        }
        for (String id : ids) {
            availablePlugins.add(disabledPluginInfo(id, file.getName()));
        }
    }

    private PluginInfo disabledPluginInfo(String id, String sourceFile) {
        String[] info = preferences.get(INFO_KEY_PREFIX + id, "").split(INFO_SEPARATOR, -1);
        if (info.length == 4) {
            return new PluginInfo(id, info[0], info[1], info[2], false, sourceFile);
        }
        return new PluginInfo(id, id, "unknown", "unknown", false, sourceFile);
    }

    private boolean isPluginEnabled(String id) {
        String enabled = preferences.get(PLUGIN_ENABLED_KEY_PREFIX + id, null);
        if (enabled != null) {
            return Boolean.parseBoolean(enabled);
        }

        String[] info = preferences.get(INFO_KEY_PREFIX + id, "").split(INFO_SEPARATOR, -1);
        if (info.length == 4 && !preferences.getBoolean(FILE_ENABLED_KEY_PREFIX + info[3], true)) {
            preferences.putBoolean(PLUGIN_ENABLED_KEY_PREFIX + id, false);
            return false;
        }
        return true;
    }

    private Set<String> getClassNames(File file) {
        Set<String> classNames = new LinkedHashSet<>();
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.endsWith(".class")
                        && !name.startsWith("META-INF/") && !name.equals("module-info.class")) {
                    classNames.add(name.replace('/', '.').substring(0, name.length() - 6));
                }
            }
        } catch (Exception exception) {
            Main.INSTANCE.getLogger().err("Could not inspect plugin " + file.getName());
        }
        return classNames;
    }

    private void cachePluginInfo(PluginInfo plugin) {
        preferences.put(INFO_KEY_PREFIX + plugin.id(), plugin.name() + INFO_SEPARATOR
                + plugin.version() + INFO_SEPARATOR + plugin.author() + INFO_SEPARATOR + plugin.sourceFile());
        String key = FILE_PLUGIN_IDS_KEY_PREFIX + plugin.sourceFile();
        String ids = preferences.get(key, "");
        for (String existingId : ids.split("\\|")) {
            if (existingId.equals(plugin.id())) return;
        }
        preferences.put(key, ids.isEmpty() ? plugin.id() : ids + "|" + plugin.id());
    }

    public void fileLoaded(Map<String, ClassNode> classes) {
        notifyPlugins(plugin -> plugin.loadFile(classes));
    }

    public void loadProgress(String fileName, int progress) {
        notifyPlugins(plugin -> plugin.loadProgress(fileName, progress));
    }

    public void classSelected(ClassNode classNode) {
        notifyPlugins(plugin -> plugin.classSelected(classNode));
    }

    public void methodSelected(ClassNode classNode, MethodNode method) {
        notifyPlugins(plugin -> plugin.methodSelected(classNode, method));
    }

    private void notifyPlugins(Consumer<Plugin> callback) {
        for (Plugin plugin : plugins) {
            try {
                callback.accept(plugin);
            } catch (Throwable throwable) {
                Main.INSTANCE.getLogger().err("Plugin " + plugin.getName() + " failed to handle an event");
                throwable.printStackTrace();
            }
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
        availablePlugins.clear();
    }
}
