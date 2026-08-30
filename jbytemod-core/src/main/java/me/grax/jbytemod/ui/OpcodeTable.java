package me.grax.jbytemod.ui;

import me.grax.jbytemod.utils.ErrorDisplay;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.*;

public class OpcodeTable extends JEditorPane {
    public OpcodeTable() {
        this.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        this.setEditable(false);
        this.setContentType("text/html");
        this.setText(loadTable());
    }

    private String loadTable() {
        try {
            return IOUtils.toString(this.getClass().getResourceAsStream("/resources/html/optable.html"));
        } catch (Exception e) {
            new ErrorDisplay(e);
            return "";
        }
    }
}
