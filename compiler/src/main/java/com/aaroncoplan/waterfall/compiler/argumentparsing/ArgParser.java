package com.aaroncoplan.waterfall.compiler.argumentparsing;

import com.aaroncoplan.waterfall.compiler.target.Backends;
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

        argumentParser.addArgument("--target")
                .setDefault(Backends.DEFAULT_TARGET)
                .help("Target language to emit. Known: " + Backends.knownTargetsList());

        try {
            final Namespace namespace = argumentParser.parseArgs(args);
            String target = namespace.getString("target");
            if (Backends.forTarget(target) == null) {
                return new Pair<>(null, "Unknown target '" + target
                        + "'. Known targets: " + Backends.knownTargetsList());
            }
            return new Pair<>(new Arguments(namespace.getList("files"), target), null);
        } catch(ArgumentParserException e) {
            final String errorMessage = e instanceof HelpScreenException ? "" : e.getMessage();
            return new Pair<>(null, errorMessage);
        }
    }

}
