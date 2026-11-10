package org.heigit.geovisroute.coordinates_generator.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public abstract class CommandLineParser {
    protected final Options options;
    protected final CommandLine cmd;

    protected CommandLineParser(String[] args) {
        this.options = new Options();
        initOptions();
        this.cmd = initCommandLine(args);
    }

    protected final void initOptions() {
        setupOptions();
    }

    protected abstract void setupOptions();
    
    protected final CommandLine initCommandLine(String[] args) {
        return parseCommandLine(args);
    }

    protected abstract CommandLine parseCommandLine(String[] args);

    protected String[] parseProfiles(String profilesInput) {
        if (profilesInput == null || profilesInput.isBlank()) {
            throw new IllegalArgumentException("Profiles must not be empty");
        }

        // 同时按逗号和空格拆分，以支持两种格式
        String[] profiles = profilesInput.split("[,\\s]+");

        // 移除空条目并去除前后空白
        return java.util.Arrays.stream(profiles)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    public abstract void printHelp();

    public boolean hasHelp() {
        return cmd == null || cmd.hasOption("h");

    }

    public String getOutputFile() {
        return cmd.getOptionValue("o", getDefaultOutputFile());
    }

    protected abstract String getDefaultOutputFile();
}
