package com.aaroncoplan.waterfall.compiler;

import java.util.ArrayList;

import com.aaroncoplan.waterfall.parser.ErrorHandler;
import com.aaroncoplan.waterfall.parser.parsing.FileParser;
import com.aaroncoplan.waterfall.parser.parsing.ParseResult;
import javafx.util.Pair;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        // this is just a test of the logger
        logger.info("Starting Waterfall Compiler");

        final Pair<Namespace, String> argParseResult = ArgParser.parseCommandLineArgs(args);
        final Namespace namespace = argParseResult.getKey();
        final String errorMsg = argParseResult.getValue();
        // if namespace is null, there is an error
        // print out the error message and exit
        if(namespace == null) {
            System.out.println(errorMsg);
            return;
        }

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
