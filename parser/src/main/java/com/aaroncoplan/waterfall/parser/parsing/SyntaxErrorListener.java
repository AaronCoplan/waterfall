package com.aaroncoplan.waterfall.parser.parsing;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;

class SyntaxErrorListener extends BaseErrorListener {
    private final String fileName;
    private final List<String> syntaxErrors;

    public SyntaxErrorListener(String fileName) {
        this.fileName = fileName;
        this.syntaxErrors = new ArrayList<>();
    }

    public List<String> getSyntaxErrors() {
        return syntaxErrors;
    }

    @Override
    public void syntaxError(
        Recognizer<?, ?> recognizer,
        Object offendingSymbol,
        int line,
        int charPositionInLine,
        String msg,
        RecognitionException e
    ) throws ParseCancellationException {
        final String errorMessage = fileName + " line " + line + ":" + charPositionInLine + " " + msg;
        syntaxErrors.add(errorMessage);
    }

}

