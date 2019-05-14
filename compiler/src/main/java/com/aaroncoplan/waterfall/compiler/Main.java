package com.aaroncoplan.waterfall.compiler;

import com.aaroncoplan.waterfall.compiler.argumentparsing.Arguments;
import com.aaroncoplan.waterfall.compiler.argumentparsing.ArgParser;
import com.aaroncoplan.waterfall.parser.ErrorHandler;
import com.aaroncoplan.waterfall.parser.FileUtils;
import com.aaroncoplan.waterfall.parser.parsing.FileParser;
import com.aaroncoplan.waterfall.parser.parsing.ParseResult;
import javafx.util.Pair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("[START] Argument Parsing");
        final Pair<Arguments, String> argParseResult = ArgParser.parseCommandLineArgs(args);
        final Arguments arguments = argParseResult.getKey();
        final String errorMsg = argParseResult.getValue();
        // if namespace is null, there is an error
        // print out the error message and exit
        if(arguments == null) {
            System.out.println(errorMsg);
            return;
        }
        logger.info("[END] Argument Parsing");

        // first check that each of the files exists
        logger.info("[START] Existence Check");
        for(String filePath : arguments.getFiles()) {
            if(!FileUtils.isReadableFile(filePath)) {
                System.out.format("File %s is not a valid readable file on disk", filePath).println();
                return;
            }
        }
        logger.info("[END] Existence Check");

        for (String filePath : arguments.getFiles()) {
            System.out.println(filePath);
            final ParseResult parseResult = FileParser.parseFile(filePath);
            if (parseResult.hasErrors()) {
                ErrorHandler.exit(
                        "[ERROR] File %s has %d errors.",
                        filePath,
                        parseResult.getSyntaxErrors().size()
                );
            }
        }
    }

}
