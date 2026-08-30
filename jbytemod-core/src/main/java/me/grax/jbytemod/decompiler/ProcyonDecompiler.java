package me.grax.jbytemod.decompiler;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.ui.DecompilerPanel;
import org.objectweb.asm.tree.MethodNode;

import java.io.StringWriter;
import java.lang.reflect.Field;

public class ProcyonDecompiler extends Decompiler {

    public ProcyonDecompiler(JByteMod jbm, DecompilerPanel dp) {
        super(jbm, dp);
    }

    public String decompile(byte[] b, MethodNode mn) {
        try {
            DecompilerSettings settings = createDecompilerSettings();
            settings.setShowSyntheticMembers(true);

            MetadataSystem metadataSystem = createMetadataSystem(b, mn);
            TypeReference type = metadataSystem.lookupType(cn.name);
            DecompilationOptions decompilationOptions = createDecompilationOptions();

            TypeDefinition resolvedType = resolveType(type);
            if (resolvedType == null) {
                return "Unable to resolve type.";
            }

            StringWriter stringWriter = new StringWriter();
            settings.getLanguage().decompileType(resolvedType, new PlainTextOutput(stringWriter), decompilationOptions);
            return stringWriter.toString();
        } catch (Exception e) {
            return e.getStackTrace().toString();
        }
    }

    private DecompilerSettings createDecompilerSettings() throws IllegalAccessException {
        DecompilerSettings settings = new DecompilerSettings();
        for (Field f : settings.getClass().getDeclaredFields()) {
            if (f.getType() == boolean.class && f.getName().startsWith("procyon")) {
                f.setAccessible(true);
                f.setBoolean(settings, Main.INSTANCE.getJByteMod().getOptions().get(f.getName()).getBoolean());
            }
        }
        return settings;
    }

    private MetadataSystem createMetadataSystem(byte[] b, MethodNode mn) {
        return new MetadataSystem(new ITypeLoader() {
            private InputTypeLoader backLoader = new InputTypeLoader();

            @Override
            public boolean tryLoadType(String s, Buffer buffer) {
                if (s.equals(cn.name)) {
                    buffer.putByteArray(b, 0, b.length);
                    buffer.position(0);
                    return true;
                } else {
                    return backLoader.tryLoadType(s, buffer);
                }
            }
        });
    }

    private DecompilationOptions createDecompilationOptions() {
        DecompilationOptions decompilationOptions = new DecompilationOptions();
        decompilationOptions.setSettings(DecompilerSettings.javaDefaults());
        decompilationOptions.setFullDecompilation(true);
        return decompilationOptions;
    }

    private TypeDefinition resolveType(TypeReference type) {
        if (type == null) {
            return null;
        }
        return type.resolve();
    }

}
