package com.aaroncoplan.waterfall.parser.argumentparsing;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import com.aaroncoplan.waterfall.parser.ErrorHandler;

public class ArgParser {

    public static Namespace parseCommandLineArgs(String[] args) {
        final ArgumentParser argumentParser = ArgumentParsers.newFor(
            "waterfall"
        ).build().defaultHelp(true).description("Waterfall programming language");
        argumentParser.addArgument("files").nargs("+").help("List of files to compile");

        try {
            return argumentParser.parseArgs(args);
        } catch(ArgumentParserException e) {
            if (!(e instanceof HelpScreenException)) {
                System.out.println(e.getMessage());
                argumentParser.printHelp();
            }
            ErrorHandler.exit();
            return null;
        }
    }

}

