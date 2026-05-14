package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.DuplicateDeclarationException;
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
            case DEC_LITERAL: return "dec";
            case STRING_LITERAL:
                // TODO(audit): no first-class string type yet; defaults to char so the
                // C backend can emit `char *`. JS/Python don't care.
                return "char";
            default:
                // TODO(audit): cross-expression type inference (identifiers, calls,
                // arithmetic) not implemented; default int.
                return "int";
        }
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        try {
            symbolTable.declare(name, inferredType);
        } catch (DuplicateDeclarationException e) {
            return new VerificationResult(false, "Duplicate declaration: " + name);
        }
        return new VerificationResult(true, null);
    }

    @Override
    public String translate(CodeGenerator backend) {
        return backend.emitUntypedVarDecl(this);
    }
}
