package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.DuplicateDeclarationException;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator;
import com.aaroncoplan.waterfall.compiler.typesystem.PrimitiveTypes;
import com.aaroncoplan.waterfall.parser.Pair;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionImplementationData extends TranslatableStatement {
    public final String name, returnType;
    public final List<Pair<String, String>> typedArguments;
    public final List<TranslatableStatement> statements;

    public FunctionImplementationData(String filePath, WaterfallParser.FunctionImplementationContext ctx) {
        super(filePath, ctx);
        this.name = ctx.name.getText();
        this.returnType = ctx.returnType == null ? null : ctx.returnType.getText();
        List<WaterfallParser.TypedArgumentContext> typedArgumentsContext = ctx.typedArgumentList() == null
                ? Collections.emptyList()
                : ctx.typedArgumentList().typedArgument();
        this.typedArguments = typedArgumentsContext.stream()
                .map(arg -> new Pair<>(arg.type().getText(), arg.name.getText()))
                .collect(Collectors.toList());
        this.statements = StatementDispatcher.fromStatementBlock(filePath, ctx.statementBlock());
    }

    @Override
    public VerificationResult verify(SymbolTable symbolTable) {
        if (returnType != null && !PrimitiveTypes.isPrimitive(returnType)) {
            return new VerificationResult(false,
                    "Illegal return type '" + returnType + "'. Known: " + PrimitiveTypes.ALL);
        }
        // Self-declaration into the outer (module) scope. Catches duplicate top-level
        // names; also makes the function visible to itself for recursion.
        try {
            symbolTable.declare(name, returnType == null ? "void" : returnType);
        } catch (DuplicateDeclarationException e) {
            return new VerificationResult(false, "Duplicate top-level declaration: " + name);
        }

        SymbolTable functionSymbolTable = new SymbolTable(symbolTable);
        for (Pair<String, String> arg : typedArguments) {
            if (!PrimitiveTypes.isPrimitive(arg.firstVal)) {
                return new VerificationResult(false,
                        "Illegal argument type '" + arg.firstVal + "' for arg " + arg.secondVal
                                + ". Known: " + PrimitiveTypes.ALL);
            }
            try {
                functionSymbolTable.declare(arg.secondVal, arg.firstVal);
            } catch (DuplicateDeclarationException e) {
                return new VerificationResult(false, "Could not declare function arg " + arg.secondVal + ", name already taken!");
            }
        }

        for (TranslatableStatement statement : statements) {
            VerificationResult r = statement.verify(functionSymbolTable);
            if (!r.isSuccessful()) return r;
        }
        return new VerificationResult(true, null);
    }

    @Override
    public String translate(CodeGenerator backend) {
        return backend.emitFunctionImpl(this);
    }
}
