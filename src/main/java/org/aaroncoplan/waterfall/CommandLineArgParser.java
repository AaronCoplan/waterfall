package org.aaroncoplan.waterfall;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class CommandLineArgParser {

    public static void parse(String[] args) {
        final ArgumentParser argumentParser = ArgumentParsers.newFor("waterfall").build(

        ).defaultHelp(true).description("Waterfall programming language");

        argumentParser.addArgument("files").nargs("+").help("List of files to compile");

        try {
            final Namespace namespace = argumentParser.parseArgs(args);
        } catch(HelpScreenException e) // do nothing
        {} catch(ArgumentParserException e) {
            argumentParser.printHelp();
            System.out.println("[ERROR] " + e.getMessage());
            System.exit(-1);
        }
    }

}

