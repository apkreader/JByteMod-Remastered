package me.grax.jbytemod.utils.attach;

import de.xbrowniecodez.jbytemod.JByteMod;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class InjectUtils {
    private static final byte[] BUFFER = new byte[4096 * 1024];

    public static void copyItself(File source, File dest) throws IOException {
        ZipFile war = new ZipFile(source);
        ZipOutputStream append = new ZipOutputStream(new FileOutputStream(dest));
        Enumeration<? extends ZipEntry> entries = war.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            if (!e.getName().equals("META-INF/MANIFEST.MF")) {
                append.putNextEntry(e);
                if (!e.isDirectory()) {
                    copy(war.getInputStream(e), append);
                }
                append.closeEntry();
            }
        }
        ZipEntry e = new ZipEntry("META-INF/MANIFEST.MF");
        append.putNextEntry(e);
        append.write(("Manifest-Version: 1.0\nAgent-Class: " + JByteMod.class.getName()
                + "\nCan-Redefine-Classes: true\nCan-Retransform-Classes: true\nCan-Set-Native-Method-Prefix: false\n").getBytes());
        append.closeEntry();
        war.close();
        append.close();
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(BUFFER)) != -1) {
            output.write(BUFFER, 0, bytesRead);
        }
    }
}
