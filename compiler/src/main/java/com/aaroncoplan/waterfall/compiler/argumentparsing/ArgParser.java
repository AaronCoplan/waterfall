package com.aaroncoplan.waterfall.compiler.argumentparsing;

import com.aaroncoplan.waterfall.parser.Pair;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class ArgParser {

    public static Pair<Arguments, String> parseCommandLineArgs(String[] args) {
        final ArgumentParser argumentParser = ArgumentParsers.newFor("waterfall")
                .build()
                .defaultHelp(true)
                .description("Waterfall programming language");

        argumentParser.addArgument("files")
                .nargs("+")
                .help("List of files to compile");

        try {
            final Namespace namespace = argumentParser.parseArgs(args);
            return new Pair<>(new Arguments(namespace.getList("files")), null);
        } catch(ArgumentParserException e) {
            final String errorMessage = e instanceof HelpScreenException ? "" : e.getMessage();
            return new Pair<>(null, errorMessage);
        }
    }

}

