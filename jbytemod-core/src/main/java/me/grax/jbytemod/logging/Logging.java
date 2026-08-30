package me.grax.jbytemod.logging;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.ui.NotificationManager;
import de.xbrowniecodez.jbytemod.JByteMod;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logging extends PrintStream {

    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
    private GuiLogging gl;
    public Logging() {
        super(new ByteArrayOutputStream(), true);
    }

    public void log(String text) {
        logConsole(getPrefix(Level.INFO), text);
    }

    public void logNotification(String text) {
        logConsole(getPrefix(Level.INFO), text);
        NotificationManager.showNotification(text);
    }

    public void warn(String text) {
        logConsole(getPrefix(Level.WARN), text);
    }

    public void err(String text) {
        logConsole(getPrefix(Level.ERROR), text);
    }

    public String getPrefix(Level l) {
        return "[" + format.format(new Date()) + "] [" + l.name() + "]";
    }

    private void logConsole(String prefix, String text) {
        System.out.print(prefix);
        System.out.print(" ");
        System.out.println(text);

        if (gl != null) {
            gl.interrupt();
        }
        gl = new GuiLogging(text);
        gl.start();
    }

    @Override
    public void println(String x) {
        log(x);
    }

    public enum Level {
        WARN, INFO, ERROR
    }

    private static class GuiLogging extends Thread {

        private String text;

        public GuiLogging(String text) {
            this.text = text;
        }

        @Override
        public void run() {
            JByteMod inst = Main.INSTANCE.getJByteMod();
            if (inst != null && inst.getPageEndPanel() != null) {
                inst.getPageEndPanel().setTip(text);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                inst.getPageEndPanel().setTip(null);
            }
        }
    }
}
