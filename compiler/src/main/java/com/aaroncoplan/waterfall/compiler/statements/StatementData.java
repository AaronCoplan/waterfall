package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class StatementData extends TranslatableStatement {

    public StatementData(String filePath, ParserRuleContext parserRuleContext) {
        super(filePath, parserRuleContext);
    }
}
