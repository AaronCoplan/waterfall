package com.aaroncoplan.waterfall.parser;

import java.util.ArrayList;

import com.aaroncoplan.waterfall.parser.argumentparsing.ArgParser;
import com.aaroncoplan.waterfall.parser.parsing.FileParser;
import com.aaroncoplan.waterfall.parser.parsing.ParseResult;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {
        // this is just a test of the logger
        logger.info("Starting Waterfall Compiler");

        final Namespace namespace = ArgParser.parseCommandLineArgs(args);

        final Object files = namespace.get("files");
        if (!(files instanceof ArrayList)) {
            ErrorHandler.exit("[ERROR] Files listed to compile are not a list of strings.");
        }
        @SuppressWarnings("unchecked")
        ArrayList<String> fileList = (ArrayList<String>) files;
        for (String filePath : fileList) {
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

