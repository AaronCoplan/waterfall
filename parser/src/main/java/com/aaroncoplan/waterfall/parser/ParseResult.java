package com.aaroncoplan.waterfall.parser;

import com.aaroncoplan.waterfall.generated.WaterfallParser;

import java.util.List;

public class ParseResult {
    private final String filePath;
    private final List<String> syntaxErrors;
    private final WaterfallParser.ProgramContext programAST;

    ParseResult(String filePath, List<String> syntaxErrors, WaterfallParser.ProgramContext programAST) {
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

