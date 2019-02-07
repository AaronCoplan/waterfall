package org.aaroncoplan.waterfall;

import com.aaroncoplan.waterfall.WaterfallParser;

public class VariableAssignment {
    private final Scope scope;
    private final String variableName;

    public VariableAssignment(
        WaterfallParser.VariableAssignmentContext variableAssignmentContext,
        Scope scope
    ) {
        this.scope = scope;
        this.variableName = variableAssignmentContext.variableName().getText();
    }

    public Scope getScope() {
        return scope;
    }

    public String getVariableName() {
        return variableName;
    }

}

