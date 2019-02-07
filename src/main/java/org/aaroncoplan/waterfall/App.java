package org.aaroncoplan.waterfall;

import com.aaroncoplan.waterfall.WaterfallLexer;
import com.aaroncoplan.waterfall.WaterfallParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
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
        waterfallParser.removeErrorListeners();
        ThrowingErrorListener errorListener = new ThrowingErrorListener(waterfallCodeFilePath);
        waterfallParser.addErrorListener(errorListener);

        final WaterfallParser.ProgramContext programAST = waterfallParser.program();

        System.out.println("ERROR MESSAGES");
        for(String msg : errorListener.getSyntaxErrors()) {
            System.out.println(msg);
        }

        System.out.println(programAST.toStringTree(waterfallParser));
    }
}

class ThrowingErrorListener extends BaseErrorListener {

    private final String name;
    private final List<String> syntaxErrors;

    public ThrowingErrorListener(String name) {
        this.name = name;
        this.syntaxErrors = new ArrayList<String>();
    }

    public List<String> getSyntaxErrors() {
        return syntaxErrors;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) throws ParseCancellationException {
        final String errorMessage = name + " line " + line + ":" + charPositionInLine + " " + msg;
        syntaxErrors.add(errorMessage);
    }
}
