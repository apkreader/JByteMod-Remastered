package me.grax.jbytemod.ui;

import javax.swing.*;
import java.awt.*;

public class NoBorderSP extends JScrollPane {
    public NoBorderSP(Component c) {
        super(c);
        this.setBorder(BorderFactory.createEmptyBorder());
    }
}
