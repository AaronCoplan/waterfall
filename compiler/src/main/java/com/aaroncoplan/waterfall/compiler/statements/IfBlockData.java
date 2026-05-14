package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IfBlockData extends TranslatableStatement {

    public static class Branch {
        public final ExpressionData condition;
        public final List<TranslatableStatement> body;
        public Branch(ExpressionData condition, List<TranslatableStatement> body) {
            this.condition = condition;
            this.body = body;
        }
    }

    public final Branch ifBranch;
    public final List<Branch> elifBranches;
    public final List<TranslatableStatement> elseBody;  // null if no else

    public IfBlockData(String filePath, WaterfallParser.IfBlockContext ctx) {
        super(filePath, ctx);
        this.ifBranch = new Branch(
                new ExpressionData(filePath, ctx.expression()),
                StatementDispatcher.fromStatementBlock(filePath, ctx.statementBlock()));
        this.elifBranches = ctx.elifBlock() == null
                ? Collections.emptyList()
                : ctx.elifBlock().stream()
                    .map(eb -> new Branch(
                            new ExpressionData(filePath, eb.expression()),
                            StatementDispatcher.fromStatementBlock(filePath, eb.statementBlock())))
                    .collect(Collectors.toList());
        if (ctx.elseBlock() == null) {
            this.elseBody = null;
        } else {
            this.elseBody = StatementDispatcher.fromStatementBlock(filePath, ctx.elseBlock().statementBlock());
        }
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        // TODO(audit): condition is not type-checked; phase 5+ should require bool.
        SymbolTable scope = new SymbolTable(symbolTable);
        for (TranslatableStatement s : ifBranch.body) {
            VerificationResult r = s.verify(scope);
            if (!r.isSuccessful()) return r;
        }
        for (Branch elif : elifBranches) {
            SymbolTable elifScope = new SymbolTable(symbolTable);
            for (TranslatableStatement s : elif.body) {
                VerificationResult r = s.verify(elifScope);
                if (!r.isSuccessful()) return r;
            }
        }
        if (elseBody != null) {
            SymbolTable elseScope = new SymbolTable(symbolTable);
            for (TranslatableStatement s : elseBody) {
                VerificationResult r = s.verify(elseScope);
                if (!r.isSuccessful()) return r;
            }
        }
        return new VerificationResult(true, null);
    }

    @Override
    public String translate() {
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(ifBranch.condition.translate()).append(") {");
        sb.append(ifBranch.body.stream().map(TranslatableStatement::translate).collect(Collectors.joining("\n")));
        sb.append("}");
        for (Branch elif : elifBranches) {
            sb.append(" else if (").append(elif.condition.translate()).append(") {");
            sb.append(elif.body.stream().map(TranslatableStatement::translate).collect(Collectors.joining("\n")));
            sb.append("}");
        }
        if (elseBody != null) {
            sb.append(" else {");
            sb.append(elseBody.stream().map(TranslatableStatement::translate).collect(Collectors.joining("\n")));
            sb.append("}");
        }
        return sb.toString();
    }
}
