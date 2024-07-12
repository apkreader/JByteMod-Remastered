package de.xbrowniecodez.jbytemod;

import de.xbrowniecodez.jbytemod.utils.update.UpdateChecker;
import lombok.Getter;
import lombok.SneakyThrows;
import me.grax.jbytemod.discord.Discord;
import me.grax.jbytemod.logging.Logging;

import me.grax.jbytemod.utils.FileUtils;
import org.apache.commons.cli.*;


import javax.swing.*;
import java.io.File;
@Getter
public enum Main {
    INSTANCE;
    private JByteMod jByteMod;
    private Logging logger;
    private Discord discord;
    private UpdateChecker updateChecker;

    public static void main(String[] args) { Main.INSTANCE.start(args); }
    @SneakyThrows
    private void start(String[] args) {
        CommandLine cmd = parseCommandLine(args);
        if (cmd.hasOption("help")) {
            this.printHelpAndExit();
        }
        this.logger = new Logging();
        this.jByteMod = new JByteMod(false);
        this.discord = new Discord("1184572566795468881");
        this.loadFileIfNeeded(cmd, jByteMod);
        SwingUtilities.invokeLater(() -> this.jByteMod.setVisible(true));
        this.updateChecker = new UpdateChecker();
    }

    private CommandLine parseCommandLine(String[] args) {
        org.apache.commons.cli.Options options = buildCommandLineOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("An error occurred while parsing the commandline ");
        }
    }


    private org.apache.commons.cli.Options buildCommandLineOptions() {
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        options.addOption("f", "file", true, "File to open");
        options.addOption("d", "dir", true, "Working directory");
        options.addOption("c", "config", true, "Config file name");
        options.addOption("?", "help", false, "Prints this help");
        return options;
    }

    private void printHelpAndExit() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(Main.INSTANCE.getJByteMod().getTitle(), buildCommandLineOptions());
        System.exit(0);
    }

    private void loadFileIfNeeded(CommandLine cmd, JByteMod frame) {
        if (cmd.hasOption("f")) {
            File input = new File(cmd.getOptionValue("f"));
            if (FileUtils.exists(input) && FileUtils.isType(input, ".jar", ".class")) {
                frame.loadFile(input);
                  Main.INSTANCE.getLogger().log("Specified file loaded");
            } else {
                 Main.INSTANCE.getLogger().err("Specified file not found");
            }
        }
    }
}
