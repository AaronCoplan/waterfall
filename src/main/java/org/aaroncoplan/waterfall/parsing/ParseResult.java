package org.aaroncoplan.waterfall.parsing;

import com.aaroncoplan.waterfall.WaterfallParser;

import java.util.List;

public class ParseResult {
    private final String filePath;
    private final List<String> syntaxErrors;
    private final WaterfallParser.ProgramContext programAST;

    public ParseResult(
        String filePath,
        List<String> syntaxErrors,
        WaterfallParser.ProgramContext programAST
    ) {
        this.filePath = filePath;
        this.syntaxErrors = syntaxErrors;
        this.programAST = programAST;
    }

}

