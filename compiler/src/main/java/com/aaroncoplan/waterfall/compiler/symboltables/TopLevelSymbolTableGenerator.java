package com.aaroncoplan.waterfall.compiler.symboltables;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.helpers.FunctionImplementationHelper;
import com.aaroncoplan.waterfall.compiler.helpers.TypedVariableDeclarationAndAssignmentHelper;

public class TopLevelSymbolTableGenerator {

    public static SymbolTable generateFromModule(WaterfallParser.ModuleContext module) {
        final SymbolTable symbolTable = new SymbolTable(null);
        final String moduleName = module.name.getText();

        for(WaterfallParser.TopLevelDeclarationContext topLevelDeclaration : module.topLevelDeclaration()) {
            if(topLevelDeclaration.typedVariableDeclarationAndAssignment() != null) {
                WaterfallParser.TypedVariableDeclarationAndAssignmentContext typedVariableDeclarationAndAssignment = topLevelDeclaration.typedVariableDeclarationAndAssignment();
                TypedVariableDeclarationAndAssignmentHelper.TypedVariableDeclarationAndAssignmentData typedVariableDeclarationAndAssignmentData = TypedVariableDeclarationAndAssignmentHelper.extractData(typedVariableDeclarationAndAssignment);
                try{
                    symbolTable.declare(typedVariableDeclarationAndAssignmentData.name, typedVariableDeclarationAndAssignmentData.type);
                } catch (DuplicateDeclarationException e) {
                    int line = typedVariableDeclarationAndAssignment.start.getLine();
                    int charPosition = typedVariableDeclarationAndAssignment.start.getCharPositionInLine();
                    System.out.format("Duplicate declaration when declaring %s in %s at %d:%d", typedVariableDeclarationAndAssignmentData.name, moduleName, line, charPosition).println();
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
