package com.aaroncoplan.waterfall.parser;

import com.aaroncoplan.waterfall.generated.WaterfallLexer;
import com.aaroncoplan.waterfall.generated.WaterfallParser;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class FileParser {

    public static ParseResult parseFile(final String filePath) {
        final String fileContents = FileUtils.readFile(filePath);
        return parseCodeString(filePath, fileContents);
    }

    public static ParseResult parseCodeString(final String filePath, final String codeString) {
        final CharStream charStream = CharStreams.fromString(codeString);
        final WaterfallLexer waterfallLexer = new WaterfallLexer(charStream);
        waterfallLexer.removeErrorListeners();
        final CommonTokenStream tokenStream = new CommonTokenStream(waterfallLexer);

        final WaterfallParser waterfallParser = new WaterfallParser(tokenStream);
        waterfallParser.removeErrorListeners();
        final SyntaxErrorListener errorListener = new SyntaxErrorListener(filePath);
        waterfallParser.addErrorListener(errorListener);

        final WaterfallParser.ProgramContext programAST = waterfallParser.program();
        final List<String> syntaxErrors = errorListener.getSyntaxErrors();
        return new ParseResult(filePath, syntaxErrors, programAST);
    }
}

