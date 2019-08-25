package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;

public class TypedVariableDeclarationAndAssignmentData extends TranslatableStatement {
    public final String name, type;
    public final int value;

    public TypedVariableDeclarationAndAssignmentData(String filePath, WaterfallParser.TypedVariableDeclarationAndAssignmentContext typedVariableDeclarationAndAssignmentContext) {
        super(filePath, typedVariableDeclarationAndAssignmentContext);
        this.name = typedVariableDeclarationAndAssignmentContext.name.getText();
        this.type = typedVariableDeclarationAndAssignmentContext.type().getText();
        this.value = Integer.parseInt(typedVariableDeclarationAndAssignmentContext.INT_LITERAL().getText());
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        if("int".equals(type)) {
            return new VerificationResult(true, null);
        } else {
            return new VerificationResult(false, "Type should be int");
        }
    }

    @Override
    public String translate() {
        return String.format("%s %s = %d;", type, name, value);
    }
}
