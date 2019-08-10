package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;

public class VariableAssignmentData extends TranslatableStatement {
    public final String name;
    public final int value;

    public VariableAssignmentData(final String filePath, WaterfallParser.VariableAssignmentContext variableAssignmentContext) {
        super(filePath, variableAssignmentContext);
        this.name = variableAssignmentContext.name.getText();
        this.value = Integer.parseInt(variableAssignmentContext.INT_LITERAL().getText());
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        return new VerificationResult(true, null);
    }

    @Override
    public String translate() {
        return String.format("%s = %d;", name, value);
    }
}
