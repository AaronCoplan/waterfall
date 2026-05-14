package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator;

public class ReturnStatementData extends TranslatableStatement {

    public final ExpressionData value;  // null for bare `return`

    public ReturnStatementData(String filePath, WaterfallParser.ReturnStatementContext ctx) {
        super(filePath, ctx);
        this.value = ctx.expression() == null ? null : new ExpressionData(filePath, ctx.expression());
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        return new VerificationResult(true, null);
    }

    @Override
    public String translate(CodeGenerator backend) {
        return backend.emitReturnStatement(this);
    }
}
