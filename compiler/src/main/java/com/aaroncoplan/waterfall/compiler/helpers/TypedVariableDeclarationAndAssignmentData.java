package com.aaroncoplan.waterfall.compiler.helpers;

public class TypedVariableDeclarationAndAssignmentData {
    public final String name, type;
    public final int value;

    TypedVariableDeclarationAndAssignmentData(String name, String type, int value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }
}
