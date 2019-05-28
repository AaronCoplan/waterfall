package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition;
import com.aaroncoplan.waterfall.compiler.statements.helpers.Translatable;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.DuplicateDeclarationException;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.parser.Pair;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionImplementationData extends TranslatableStatement {
    public final String name, returnType;
    public final List<Pair<String, String>> typedArguments;
    public final List<TranslatableStatement> statements;

    public FunctionImplementationData(String filePath, WaterfallParser.FunctionImplementationContext functionImplementationContext) {
        super(filePath, functionImplementationContext);
        this.name = functionImplementationContext.name.getText();
        this.returnType = functionImplementationContext.returnType == null ? null : functionImplementationContext.returnType.getText();
        List<WaterfallParser.TypedArgumentContext> typedArgumentsContext = functionImplementationContext.typedArgumentList() == null ? Collections.emptyList() : functionImplementationContext.typedArgumentList().typedArgument();
        this.typedArguments = typedArgumentsContext.stream().map(arg -> new Pair<>(arg.type().getText(), arg.name.getText())).collect(Collectors.toList());
        List<WaterfallParser.StatementContext> statementContexts = functionImplementationContext.statement() == null ? Collections.emptyList() : functionImplementationContext.statement();
        this.statements = statementContexts.stream().map(statementContext -> {
            if(statementContext.typedVariableDeclarationAndAssignment() != null) {
                return new TypedVariableDeclarationAndAssignmentData(filePath, statementContext.typedVariableDeclarationAndAssignment());
            } else if(statementContext.untypedVariableDeclarationAndAssignment() != null) {
                return new UntypedVariableDeclarationAndAssignmentData(filePath, statementContext.untypedVariableDeclarationAndAssignment());
            } else if(statementContext.variableAssignment() != null) {
                return new VariableAssignmentData(filePath, statementContext.variableAssignment());
            } else {
                throw new RuntimeException("UNRECOGNIZED STATEMENT");
            }
        }).collect(Collectors.toList());
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        if(returnType != null && !"int".equals(returnType)) {
            return new VerificationResult(false, "Illegal return type " + returnType);
        }

        // create a symbol table and declare the arguments
        SymbolTable functionSymbolTable = new SymbolTable(symbolTable);
        for(Pair<String, String> arg : typedArguments) {
            if(!"int".equals(arg.firstVal)) {
                return new VerificationResult(false, "Illegal argument type " + arg.firstVal + " for arg " + arg.secondVal);
            } else {
                try {
                    functionSymbolTable.declare(arg.secondVal, arg.firstVal);
                } catch(DuplicateDeclarationException e) {
                    return new VerificationResult(false, "Could not declare function arg " + arg.secondVal + ", name already taken!");
                }
            }
        }

        for(TranslatableStatement translatableStatement : statements) {
            VerificationResult verificationResult = translatableStatement.verify(functionSymbolTable);
            if(!verificationResult.isSuccessful()) {
                return verificationResult;
            }
        }
        return new VerificationResult(true, null);
    }

    @Override
    public String translate() {
        final String translatedReturnType = returnType == null ? "void" : returnType;
        final String args = typedArguments.stream().map(arg -> String.format("%s %s", arg.firstVal, arg.secondVal)).collect(Collectors.joining(", "));
        final String functionBody = statements.stream().map(TranslatableStatement::translate).collect(Collectors.joining("\n"));
        return String.format("%s %s(%s) {%s}", translatedReturnType, name, args, functionBody);
    }
}
