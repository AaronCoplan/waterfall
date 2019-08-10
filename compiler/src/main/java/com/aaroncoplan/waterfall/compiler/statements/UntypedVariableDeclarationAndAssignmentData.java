package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;

public class UntypedVariableDeclarationAndAssignmentData extends TranslatableStatement {
    public final String name, type;
    public final int value;

    public UntypedVariableDeclarationAndAssignmentData(final String filePath, WaterfallParser.UntypedVariableDeclarationAndAssignmentContext untypedVariableDeclarationAndAssignmentContext) {
        super(filePath, untypedVariableDeclarationAndAssignmentContext);
        this.name = untypedVariableDeclarationAndAssignmentContext.name.getText();
        this.type = "int";
        this.value = Integer.parseInt(untypedVariableDeclarationAndAssignmentContext.INT_LITERAL().getText());
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        return new VerificationResult(true, null);
    }

    @Override
    public String translate() {
        return String.format("%s %s = %d;", type, name, value);
    }
}
