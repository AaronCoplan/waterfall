package com.aaroncoplan.waterfall.compiler.statements.helpers;

import org.antlr.v4.runtime.ParserRuleContext;

public abstract class TranslatableStatement {

    private final SourcePosition sourcePosition;


    public TranslatableStatement(String filePath, ParserRuleContext parserRuleContext) {
        this.sourcePosition = new SourcePosition(filePath, parserRuleContext.start.getLine(), parserRuleContext.start.getCharPositionInLine());
    }

    public abstract VerificationResult verify();
    public abstract String translate();

    public SourcePosition getSourcePosition() {
        return sourcePosition;
    }
}
