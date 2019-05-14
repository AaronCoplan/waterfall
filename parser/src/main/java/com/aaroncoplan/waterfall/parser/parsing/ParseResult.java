package com.aaroncoplan.waterfall.parser.parsing;

import com.aaroncoplan.waterfall.WaterfallParser;

import java.util.List;

public class ParseResult {
    private final String filePath;
    private final List<String> syntaxErrors;
    private final WaterfallParser.ProgramContext programAST;

    public ParseResult(String filePath, List<String> syntaxErrors, WaterfallParser.ProgramContext programAST) {
        this.filePath = filePath;
        this.syntaxErrors = syntaxErrors;
        this.programAST = programAST;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<String> getSyntaxErrors() {
        return syntaxErrors;
    }

    public WaterfallParser.ProgramContext getProgramAST() {
        return programAST;
    }

    public boolean hasErrors() {
        return syntaxErrors.size() > 0;
    }

}

