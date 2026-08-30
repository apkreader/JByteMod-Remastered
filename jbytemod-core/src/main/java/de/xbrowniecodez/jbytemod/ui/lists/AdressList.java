package de.xbrowniecodez.jbytemod.ui.lists;

import me.grax.jbytemod.ui.lists.MyCodeList;
import me.grax.jbytemod.ui.lists.entries.InstrEntry;
import me.grax.jbytemod.utils.gui.SwingUtils;
import me.grax.jbytemod.utils.list.LazyListModel;

import javax.swing.*;
import java.awt.*;

public class AdressList extends JList<String> {
    private final MyCodeList myCodeList;

    public AdressList(MyCodeList myCodeList) {
        super(new DefaultListModel<>());
        this.myCodeList = myCodeList;
        myCodeList.setAdressList(this);
        this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        this.updateAdr();
        this.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(-1, -1);
            }
        });
        this.setPrototypeCellValue("00000");
        //this.setFixedCellHeight(30);
        SwingUtils.disableSelection(this);
    }

    public void updateAdr() {
        LazyListModel<InstrEntry> clm = (LazyListModel<InstrEntry>) myCodeList.getModel();
        final int size = clm.getSize();
        this.setModel(new AbstractListModel<String>() {
            @Override
            public int getSize() {
                return size;
            }

            @Override
            public String getElementAt(int index) {
                return String.format("%05d", index);
            }
        });
    }
}
