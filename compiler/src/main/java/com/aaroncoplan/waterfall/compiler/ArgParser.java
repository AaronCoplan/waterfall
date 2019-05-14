package com.aaroncoplan.waterfall.compiler;

import javafx.util.Pair;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

class ArgParser {

    static Pair<Namespace, String> parseCommandLineArgs(String[] args) {
        final ArgumentParser argumentParser = ArgumentParsers.newFor("waterfall")
                .build()
                .defaultHelp(true)
                .description("Waterfall programming language");

        argumentParser.addArgument("files")
                .nargs("+")
                .help("List of files to compile");

        try {
            return new Pair<>(argumentParser.parseArgs(args), null);
        } catch(ArgumentParserException e) {
            final String errorMessage = e instanceof HelpScreenException ? "" : String.format("%s\n%s", e.getMessage(), argumentParser.formatHelp());
            return new Pair<>(null, errorMessage);
        }
    }

}

