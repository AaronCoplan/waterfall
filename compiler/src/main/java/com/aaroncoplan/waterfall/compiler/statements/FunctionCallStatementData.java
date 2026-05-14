package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator;

public class FunctionCallStatementData extends TranslatableStatement {

    public final FunctionCallData call;

    public FunctionCallStatementData(String filePath, WaterfallParser.FunctionCallContext ctx) {
        super(filePath, ctx);
        this.call = new FunctionCallData(filePath, ctx);
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        // TODO(audit): no resolution against the symbol table yet; phase 6+ will check.
        return new VerificationResult(true, null);
    }

    @Override
    public String translate(CodeGenerator backend) {
        return backend.emitFunctionCallStatement(this);
    }
}
