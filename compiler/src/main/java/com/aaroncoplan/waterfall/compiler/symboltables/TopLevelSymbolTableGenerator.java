package com.aaroncoplan.waterfall.compiler.symboltables;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.FunctionImplementationData;
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition;

public class TopLevelSymbolTableGenerator {

    public static SymbolTable generateFromModule(final String filePath, WaterfallParser.ModuleContext module) {
        final SymbolTable symbolTable = new SymbolTable(null);
        final String moduleName = module.name.getText();

        for(WaterfallParser.TopLevelDeclarationContext topLevelDeclaration : module.topLevelDeclaration()) {
            if(topLevelDeclaration.typedVariableDeclarationAndAssignment() != null) {
                TypedVariableDeclarationAndAssignmentData typedVariableDeclarationAndAssignmentData = new TypedVariableDeclarationAndAssignmentData(filePath, topLevelDeclaration.typedVariableDeclarationAndAssignment());
                try{
                    symbolTable.declare(typedVariableDeclarationAndAssignmentData.name, typedVariableDeclarationAndAssignmentData.type);
                } catch (DuplicateDeclarationException e) {
                    final SourcePosition sourcePosition = typedVariableDeclarationAndAssignmentData.getSourcePosition();
                    System.out.format("Duplicate declaration when declaring %s in %s at %d:%d", typedVariableDeclarationAndAssignmentData.name, moduleName, sourcePosition.getLine(), sourcePosition.getColumn()).println();
                    return null;
                }

            } else if(topLevelDeclaration.functionImplementation() != null) {
                FunctionImplementationData functionImplementationData = new FunctionImplementationData(filePath, topLevelDeclaration.functionImplementation());
                try {
                    symbolTable.declare(functionImplementationData.name, functionImplementationData.returnType);
                } catch (DuplicateDeclarationException e) {
                    final SourcePosition sourcePosition = functionImplementationData.getSourcePosition();
                    System.out.format("Duplicate declaration when declaring %s in %s at %d:%d", functionImplementationData.name, moduleName, sourcePosition.getLine(), sourcePosition.getColumn()).println();
                    return null;
                }
            }
        }

        return symbolTable;
    }
}
