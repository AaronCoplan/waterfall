package org.aaroncoplan.waterfall;

import com.aaroncoplan.waterfall.WaterfallParser;

public class VariableDeclaration {
    private final Scope scope;
    private final String variableType;
    private final String variableName;
    private int intValue;
    private double doubleValue;
    private String refValue = null;

    public VariableDeclaration(
        WaterfallParser.VariableDeclarationContext variableDeclarationContext,
        Scope scope
    ) {
        this.scope = scope;

        this.variableType = variableDeclarationContext.variableType().ID().getText();
        this.variableName = variableDeclarationContext.variableName().ID().getText();

        if ("int".equals(this.variableType)) {
            if (variableDeclarationContext.variableValue().INT_LITERAL() != null) {
                intValue = Integer.parseInt(
                    variableDeclarationContext.variableValue().INT_LITERAL().getText()
                );
            } else if (variableDeclarationContext.variableValue().variableName() != null) {
                refValue = variableDeclarationContext.variableValue().variableName().ID().getText();
            } else {
                throw new RuntimeException("Failed to handle something in VarDecl for int");
            }
        } else if ("dec".equals(this.variableType)) {
            if (variableDeclarationContext.variableValue().DEC_LITERAL() != null) {
                doubleValue = Double.parseDouble(
                    variableDeclarationContext.variableValue().DEC_LITERAL().getText()
                );
            } else if (variableDeclarationContext.variableValue().variableName() != null) {
                refValue = variableDeclarationContext.variableValue().variableName().ID().getText();
            } else {
                throw new RuntimeException("Failed to handle something in VarDecl for dec");
            }
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
            if (refValue == null) {
                return String.format("%s %s = %d;", "int", variableName, intValue);
            } else {
                if (scope.getReference(refValue) != null) {
                    return String.format("%s %s = %s;", "int", variableName, refValue);
                } else {
                    throw new RuntimeException("REF DOES NOT EXIST");
                }
            }
        } else {
            if (refValue == null) {
                return String.format("%s %s = %f;", "double", variableName, doubleValue);
            } else {
                if (scope.getReference(refValue) != null) {
                    return String.format("%s %s = %s;", "double", variableName, refValue);
                } else {
                    throw new RuntimeException("REF DOES NOT EXIST");
                }
            }
        }
    }

}

