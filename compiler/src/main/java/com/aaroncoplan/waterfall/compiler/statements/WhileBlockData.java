package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator;

import java.util.List;

public class WhileBlockData extends TranslatableStatement {

    public final ExpressionData condition;
    public final List<TranslatableStatement> body;

    public WhileBlockData(String filePath, WaterfallParser.WhileBlockContext ctx) {
        super(filePath, ctx);
        this.condition = new ExpressionData(filePath, ctx.expression());
        this.body = StatementDispatcher.fromStatementBlock(filePath, ctx.statementBlock());
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        SymbolTable scope = new SymbolTable(symbolTable);
        for (TranslatableStatement s : body) {
            VerificationResult r = s.verify(scope);
            if (!r.isSuccessful()) return r;
        }
        return new VerificationResult(true, null);
    }

    @Override
    public String translate(CodeGenerator backend) {
        return backend.emitWhileBlock(this);
    }
}
