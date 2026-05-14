package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;

import java.util.List;
import java.util.stream.Collectors;

public class ForBlockData extends TranslatableStatement {

    public final String iteratorName;
    public final String collectionName;
    public final List<TranslatableStatement> body;

    public ForBlockData(String filePath, WaterfallParser.ForBlockContext ctx) {
        super(filePath, ctx);
        WaterfallParser.ForInBlockContext inner = ctx.forInBlock();
        // Grammar: FOR L_PARENS name=ID IN collection=ID R_PARENS ...
        // Both labels named name/collection -> two ID() tokens at positions 0 and 1.
        this.iteratorName = inner.ID(0).getText();
        this.collectionName = inner.ID(1).getText();
        this.body = StatementDispatcher.fromStatementBlock(filePath, inner.statementBlock());
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        // TODO(audit): iterator type isn't inferred from the collection yet.
        SymbolTable scope = new SymbolTable(symbolTable);
        for (TranslatableStatement s : body) {
            VerificationResult r = s.verify(scope);
            if (!r.isSuccessful()) return r;
        }
        return new VerificationResult(true, null);
    }

    @Override
    public String translate() {
        // Legacy emitter: produce a C++-style range-for that's "close enough" to most targets.
        // Per-target backends in phase 3+ override per language.
        String inner = body.stream().map(TranslatableStatement::translate).collect(Collectors.joining("\n"));
        return String.format("for (auto %s : %s) {%s}", iteratorName, collectionName, inner);
    }
}
