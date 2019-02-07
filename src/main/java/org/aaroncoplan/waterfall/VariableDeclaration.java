package org.aaroncoplan.waterfall;

import com.aaroncoplan.waterfall.WaterfallParser;

public class VariableDeclaration {
    private final Scope scope;
    private final String variableType;
    private final String variableName;
    private int intValue;
    private double doubleValue;

    public VariableDeclaration(
        WaterfallParser.VariableDeclarationContext variableDeclarationContext,
        Scope scope
    ) {
        this.scope = scope;

        this.variableType = variableDeclarationContext.variableType().ID().getText();
        this.variableName = variableDeclarationContext.variableName().ID().getText();

        if ("int".equals(this.variableType)) {
            intValue = Integer.parseInt(
                variableDeclarationContext.variableValue().INT_LITERAL().getText()
            );
        } else if ("dec".equals(this.variableType)) {
            doubleValue = Double.parseDouble(
                variableDeclarationContext.variableValue().DEC_LITERAL().getText()
            );
        } else {
            throw new RuntimeException("UNRECOGNIZED TYPE: " + this.variableType);
        }
    }

    public Scope getScope() {
        return scope;
    }

    public String getVariableName() {
        return variableName;
    }

    public String emit() {
        if ("int".equals(this.variableType)) {
            return String.format("%s %s = %d;", "int", variableName, intValue);
        } else {
            return String.format("%s %s = %f;", "double", variableName, doubleValue);
        }
    }

}

