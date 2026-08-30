package de.xbrowniecodez.jbytemod.decompiler;

import lombok.SneakyThrows;

import de.xbrowniecodez.jbytemod.JByteMod;
import me.grax.jbytemod.decompiler.Decompiler;
import me.grax.jbytemod.ui.DecompilerPanel;

import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.util.jar.Manifest;

public class VineflowerDecompiler extends Decompiler implements IBytecodeProvider, IResultSaver {
    String content;
    byte[] byteArray;
    public VineflowerDecompiler(JByteMod jbm, DecompilerPanel dp) {
        super(jbm, dp);
    }

    @Override
    @SneakyThrows
    public String decompile(byte[] b, MethodNode mn) {
        this.byteArray = b;
        Fernflower vineflower = new Fernflower(this, this, IFernflowerPreferences.getDefaults(), new FernFlowerLogger());
        vineflower.addSource(new File(".class"));
        vineflower.decompileContext();
        return content.trim();
    }

    @Override
    public byte[] getBytecode(String externalPath, String internalPath) {
        return byteArray;
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        this.content = content;
    }

    @Override public void closeArchive(String path, String archiveName) {}
    @Override public void copyEntry(String source, String path, String archiveName, String entry) {}
    @Override public void copyFile(String source, String path, String entryName) {}
    @Override public void createArchive(String path, String archiveName, Manifest manifest) {}
    @Override public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {}
    @Override public void saveDirEntry(String path, String archiveName, String entryName) {}
    @Override public void saveFolder(String path) {}

    public static class FernFlowerLogger extends IFernflowerLogger {
        @Override
        public void writeMessage(String message, Severity severity) {}

        @Override
        public void writeMessage(String message, Severity severity, Throwable t) {}
    }
}
