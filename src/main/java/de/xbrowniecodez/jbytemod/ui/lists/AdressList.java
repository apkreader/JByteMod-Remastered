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
        LazyListModel<String> lm = new LazyListModel<>();
        LazyListModel<InstrEntry> clm = (LazyListModel<InstrEntry>) myCodeList.getModel();

        int numDigits = String.valueOf(clm.getSize() - 1).length();  // calculate based on max index

        for (int i = 0; i < clm.getSize(); i++) {
            String number = String.format("%05d", i);  // always 6 digits with leading zeros
            lm.addElement(number);
        }

        this.setModel(lm);
    }
}
