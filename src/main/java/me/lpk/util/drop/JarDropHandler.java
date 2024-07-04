package me.lpk.util.drop;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

public class JarDropHandler extends TransferHandler {
    private static final long serialVersionUID = 1232L;
    private final IDropUser user;
    private final int id;

    public JarDropHandler(IDropUser user, int id) {
        this.user = user;
        this.id = id;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        info.setShowDropLocation(false);
        return info.isDrop() && info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop())
            return false;
        Transferable t = info.getTransferable();
        List<File> data = null;
        try {
            data = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
        } catch (Exception e) {
            return false;
        }
        user.preLoadJars(id);
        for (File file : data) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".jar")
                    || fileName.endsWith(".class")) {
                user.onJarLoad(id, file);
                break;
            }
        }
        return true;
    }
}
