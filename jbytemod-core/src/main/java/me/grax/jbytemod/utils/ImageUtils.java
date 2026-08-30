package me.grax.jbytemod.utils;

import de.xbrowniecodez.jbytemod.Main;
import lombok.experimental.UtilityClass;

import java.awt.*;
import java.awt.image.BufferedImage;

@UtilityClass
public class ImageUtils {
    private final String watermark = "Created with " + Main.INSTANCE.getJByteMod().getTitle();

    public BufferedImage watermark(BufferedImage old) {
        BufferedImage copy = copyImage(old);
        Graphics2D g2d = copy.createGraphics();
        g2d.setPaint(Color.black);
        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int x = copy.getWidth() - fm.stringWidth(watermark) - 5;
        int y = fm.getHeight();
        g2d.drawString(watermark, x, y);
        g2d.dispose();
        return copy;
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage b = new BufferedImage(source.getWidth() + 60, source.getHeight() + 60, source.getType());
        Graphics g = b.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, source.getWidth() + 60, source.getHeight() + 60);
        g.setColor(Color.BLACK);
        g.drawImage(source, 30, 30, null);
        g.dispose();
        return b;
    }
}
