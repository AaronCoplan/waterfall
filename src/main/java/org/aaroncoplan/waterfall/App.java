package org.aaroncoplan.waterfall;

import com.aaroncoplan.waterfall.WaterfallLexer;
import com.aaroncoplan.waterfall.WaterfallParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class App {
    public String getGreeting() {
        return "Hello world.";
    }

    public static void main(String[] args) {
        // open file and read it in
        final String waterfallCodeFilePath = args[0];
        final StringBuilder code = new StringBuilder();
        try {
            Scanner sc = new Scanner(new File(waterfallCodeFilePath));
            while(sc.hasNextLine()) {
                code.append(sc.nextLine() + "\n");
            }
            sc.close();
        } catch (FileNotFoundException e) {
            System.err.format("File %s could not be found", waterfallCodeFilePath).println();
            System.exit(-1);
        }

        // parse using ANTLR
        final CharStream charStream = CharStreams.fromString(code.toString());
        final WaterfallLexer waterfallLexer = new WaterfallLexer(charStream);
        final CommonTokenStream tokenStream = new CommonTokenStream(waterfallLexer);
        final WaterfallParser waterfallParser = new WaterfallParser(tokenStream);

        final WaterfallParser.ProgramContext programAST = waterfallParser.program();
        System.out.println(programAST.toStringTree());
    }
}
