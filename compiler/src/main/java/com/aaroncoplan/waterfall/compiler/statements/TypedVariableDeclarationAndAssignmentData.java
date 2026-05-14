package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator;

public class TypedVariableDeclarationAndAssignmentData extends TranslatableStatement {
    public final String name, type;
    public final ExpressionData value;

    public TypedVariableDeclarationAndAssignmentData(String filePath, WaterfallParser.TypedVariableDeclarationAndAssignmentContext ctx) {
        super(filePath, ctx);
        this.name = ctx.name.getText();
        this.type = ctx.type().getText();
        this.value = new ExpressionData(filePath, ctx.expression());
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        if (!"int".equals(type)) {
            // TODO(audit): only "int" is allowed at phase 1; phase 5 relaxes for dec/bool/char.
            return new VerificationResult(false, "Type should be int");
        }
        return new VerificationResult(true, null);
    }

    @Override
    public String translate(CodeGenerator backend) {
        return backend.emitTypedVarDecl(this);
    }
}
