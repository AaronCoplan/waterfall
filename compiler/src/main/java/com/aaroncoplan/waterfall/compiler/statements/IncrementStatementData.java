package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator;

public class IncrementStatementData extends TranslatableStatement {

    public final String name;
    public final String op;  // "++" or "--"

    public IncrementStatementData(String filePath, WaterfallParser.IncrementStatementContext ctx) {
        super(filePath, ctx);
        this.name = ctx.name.getText();
        this.op = ctx.op.getText();
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        return new VerificationResult(true, null);
    }

    @Override
    public String translate(CodeGenerator backend) {
        return backend.emitIncrementStatement(this);
    }
}
