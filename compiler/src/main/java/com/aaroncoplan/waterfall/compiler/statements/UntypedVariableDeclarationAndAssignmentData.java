package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UntypedVariableDeclarationAndAssignmentData extends TranslatableStatement {
    public final String name;
    public final String inferredType;
    public final List<String> modifiers;
    public final ExpressionData value;

    public UntypedVariableDeclarationAndAssignmentData(final String filePath, WaterfallParser.UntypedVariableDeclarationAndAssignmentContext ctx) {
        super(filePath, ctx);
        this.name = ctx.name.getText();
        this.modifiers = ctx.modifier() == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(
                    ctx.modifier().stream().map(m -> m.getText()).collect(Collectors.toList()));
        this.value = new ExpressionData(filePath, ctx.expression());
        this.inferredType = inferType(this.value);
    }

    public boolean isImmutable() {
        return modifiers.contains("const") || modifiers.contains("imm");
    }

    private static String inferType(ExpressionData expr) {
        switch (expr.kind) {
            case INT_LITERAL: return "int";
            case DEC_LITERAL:
                // TODO(audit): `dec` not yet relaxed by the verifier; phase 5 adds it.
                return "int";
            case STRING_LITERAL:
                // TODO(audit): no string type yet; phase 5 will add `char[]` / `char *`.
                return "int";
            default:
                // TODO(audit): cross-expression type inference not implemented; default int.
                return "int";
        }
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        return new VerificationResult(true, null);
    }

    @Override
    public String translate(CodeGenerator backend) {
        return backend.emitUntypedVarDecl(this);
    }
}
