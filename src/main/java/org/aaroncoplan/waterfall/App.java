package org.aaroncoplan.waterfall;

import java.util.ArrayList;

import net.sourceforge.argparse4j.inf.Namespace;

import org.aaroncoplan.waterfall.argumentparsing.ArgumentParser;
import org.aaroncoplan.waterfall.parsing.FileParser;
import org.aaroncoplan.waterfall.parsing.ParseResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {
        // this is just a test of the logger
        logger.info("Starting Waterfall Compiler");

        final Namespace namespace = ArgumentParser.parseCommandLineArgs(args);

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

