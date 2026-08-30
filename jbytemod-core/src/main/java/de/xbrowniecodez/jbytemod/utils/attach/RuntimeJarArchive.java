package de.xbrowniecodez.jbytemod.utils.attach;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.BytecodeUtils;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.JarArchive;
import de.xbrowniecodez.jbytemod.utils.asm.Loader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class RuntimeJarArchive extends JarArchive {

    private Instrumentation ins;
    private ArrayList<String> systemClasses;
    private final Map<String, Class<?>> runtimeClasses = new HashMap<>();

    public RuntimeJarArchive(Instrumentation ins) {
        super(new HashMap<>(), new HashMap<>());
        this.ins = ins;
        systemClasses = new ArrayList<String>();
        try {
            loadNames(JByteMod.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            Main.INSTANCE.getLogger().log("Successfully loaded system class names");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void loadNames(String path) throws IOException {
        try (JarFile self = new JarFile(path)) {
            Enumeration<JarEntry> e = self.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    systemClasses.add(entry.getName().substring(0, name.length() - 6));
                }
            }
        }
    }

    @Override
    public Map<String, ClassNode> getClasses() {
        for (Class<?> c : ins.getAllLoadedClasses()) {
            String name = c.getName().replace('.', '/');
            if (!isRT(name) && !classes.containsKey(name)) {
                if (c.isHidden() || name.contains("$$") || systemClasses.contains(name) || name.contains("[") || !ins.isModifiableClass(c)) {
                    continue;
                }
                try {
                    ClassNode cn = Loader.classToNode(c);
                    if (cn != null) {
                        classes.put(name, cn);
                        output.put(name, BytecodeUtils.getClassNodeBytes(cn));
                        runtimeClasses.put(name, c);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return classes;
    }

    public Class<?> getRuntimeClass(String name) {
        return runtimeClasses.get(name);
    }

    private boolean isRT(String name) {
        return name.startsWith("java/") || name.startsWith("sun/") || name.startsWith("com/sun") || name.startsWith("jdk");
    }
}
