package com.aaroncoplan.waterfall.compiler.helpers;

import com.aaroncoplan.waterfall.WaterfallParser;

public class TypedVariableDeclarationAndAssignmentHelper {

    public static TypedVariableDeclarationAndAssignmentData extractData(WaterfallParser.TypedVariableDeclarationAndAssignmentContext typedVariableDeclarationAndAssignment) {
        String variableType = typedVariableDeclarationAndAssignment.type().getText();
        String variableName = typedVariableDeclarationAndAssignment.name.getText();
        int value  = Integer.parseInt(typedVariableDeclarationAndAssignment.INT_LITERAL().getText());
        return new TypedVariableDeclarationAndAssignmentHelper().new TypedVariableDeclarationAndAssignmentData(variableName, variableType, value);
    }

    public class TypedVariableDeclarationAndAssignmentData {
        public final String name, type;
        public final int value;

        private TypedVariableDeclarationAndAssignmentData(String name, String type, int value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }
}
