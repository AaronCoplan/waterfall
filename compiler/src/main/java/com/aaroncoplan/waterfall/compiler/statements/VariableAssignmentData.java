package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;

public class VariableAssignmentData extends StatementData {
    public final String name;
    public final int value;

    public VariableAssignmentData(final String filePath, WaterfallParser.VariableAssignmentContext variableAssignmentContext) {
        super(filePath, variableAssignmentContext);
        this.name = variableAssignmentContext.name.getText();
        this.value = Integer.parseInt(variableAssignmentContext.INT_LITERAL().getText());
    }

    @Override
    public VerificationResult verify() {
        return new VerificationResult(true, null);
    }

    @Override
    public String translate() {
        return String.format("%s = %d;", name, value);
    }
}
