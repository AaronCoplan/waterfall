package com.aaroncoplan.waterfall.compiler.symboltables;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.helpers.FunctionImplementationHelper;

public class TopLevelSymbolTableGenerator {

    public static SymbolTable generateFromModule(WaterfallParser.ModuleContext module) {
        final SymbolTable symbolTable = new SymbolTable(null);
        final String moduleName = module.name.getText();

        for(WaterfallParser.TopLevelDeclarationContext topLevelDeclaration : module.topLevelDeclaration()) {
            if(topLevelDeclaration.typedVariableDeclarationAndAssignment() != null) {
                WaterfallParser.TypedVariableDeclarationAndAssignmentContext typedVariableDeclarationAndAssignment = topLevelDeclaration.typedVariableDeclarationAndAssignment();
                String variableType = typedVariableDeclarationAndAssignment.type().getText();
                String variableName = typedVariableDeclarationAndAssignment.name.getText();
                try{
                    symbolTable.declare(variableName, variableType);
                } catch (DuplicateDeclarationException e) {
                    int line = typedVariableDeclarationAndAssignment.start.getLine();
                    int charPosition = typedVariableDeclarationAndAssignment.start.getCharPositionInLine();
                    System.out.format("Duplicate declaration when declaring %s in %s at %d:%d", variableName, moduleName, line, charPosition).println();
                    return null;
                }

            } else if(topLevelDeclaration.functionImplementation() != null) {
                WaterfallParser.FunctionImplementationContext functionImplementation = topLevelDeclaration.functionImplementation();
                FunctionImplementationHelper.FunctionImplementationData functionImplementationData = FunctionImplementationHelper.extractData(functionImplementation);
                try {
                    symbolTable.declare(functionImplementationData.name, functionImplementationData.returnType);
                } catch (DuplicateDeclarationException e) {
                    int line = functionImplementation.start.getLine();
                    int charPosition = functionImplementation.start.getCharPositionInLine();
                    System.out.format("Duplicate declaration when declaring %s in %s at %d:%d", functionImplementationData.name, moduleName, line, charPosition).println();
                    return null;
                }
            }
        }

        return symbolTable;
    }
}
