package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;

public class TypedVariableDeclarationAndAssignmentData extends TranslatableStatement {
    public final String name, type;
    public final int value;

    public TypedVariableDeclarationAndAssignmentData(String filePath, WaterfallParser.TypedVariableDeclarationAndAssignmentContext typedVariableDeclarationAndAssignmentContext) {
        super(filePath, typedVariableDeclarationAndAssignmentContext);
        this.name = typedVariableDeclarationAndAssignmentContext.name.getText();
        this.type = typedVariableDeclarationAndAssignmentContext.type().getText();
        this.value = Integer.parseInt(typedVariableDeclarationAndAssignmentContext.INT_LITERAL().getText());
    }
}
