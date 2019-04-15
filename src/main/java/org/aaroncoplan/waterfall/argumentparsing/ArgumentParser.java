package org.aaroncoplan.waterfall.argumentparsing;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.aaroncoplan.waterfall.ErrorHandler;

public class ArgumentParser {

    public static Namespace parseCommandLineArgs(String[] args) {
        final net.sourceforge.argparse4j.inf.ArgumentParser argumentParser = ArgumentParsers.newFor(
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

