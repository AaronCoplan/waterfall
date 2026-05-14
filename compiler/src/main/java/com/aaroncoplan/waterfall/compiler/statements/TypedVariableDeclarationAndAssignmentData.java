package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.DuplicateDeclarationException;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator;
import com.aaroncoplan.waterfall.compiler.typesystem.PrimitiveTypes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TypedVariableDeclarationAndAssignmentData extends TranslatableStatement {
    public final String name, type;
    public final List<String> modifiers;
    public final ExpressionData value;

    public TypedVariableDeclarationAndAssignmentData(String filePath, WaterfallParser.TypedVariableDeclarationAndAssignmentContext ctx) {
        super(filePath, ctx);
        this.name = ctx.name.getText();
        this.type = ctx.type().getText();
        this.modifiers = ctx.modifier() == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(
                    ctx.modifier().stream().map(m -> m.getText()).collect(Collectors.toList()));
        this.value = new ExpressionData(filePath, ctx.expression());
    }

    public boolean isImmutable() {
        return modifiers.contains("const") || modifiers.contains("imm");
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        if (!PrimitiveTypes.isPrimitive(type)) {
            return new VerificationResult(false,
                    "Type '" + type + "' is not a recognized primitive. Known: " + PrimitiveTypes.ALL);
        }
        try {
            symbolTable.declare(name, type);
        } catch (DuplicateDeclarationException e) {
            return new VerificationResult(false, "Duplicate declaration: " + name);
        }
        return new VerificationResult(true, null);
    }

    @Override
    public String translate(CodeGenerator backend) {
        return backend.emitTypedVarDecl(this);
    }
}
