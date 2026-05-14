package com.aaroncoplan.waterfall.compiler.target;

import com.aaroncoplan.waterfall.compiler.statements.ArrayLiteralData;
import com.aaroncoplan.waterfall.compiler.statements.BundleLiteralData;
import com.aaroncoplan.waterfall.compiler.statements.ExpressionData;
import com.aaroncoplan.waterfall.compiler.statements.ForBlockData;
import com.aaroncoplan.waterfall.compiler.statements.FunctionCallData;
import com.aaroncoplan.waterfall.compiler.statements.FunctionCallStatementData;
import com.aaroncoplan.waterfall.compiler.statements.FunctionImplementationData;
import com.aaroncoplan.waterfall.compiler.statements.IfBlockData;
import com.aaroncoplan.waterfall.compiler.statements.LambdaFunctionData;
import com.aaroncoplan.waterfall.compiler.statements.ModuleAst;
import com.aaroncoplan.waterfall.compiler.statements.ReturnStatementData;
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.UntypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.VariableAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.WhileBlockData;

/**
 * Pluggable target-language code generator. One implementation per supported
 * output language. Each method takes a node from the front-end AST and returns
 * its rendered fragment in the target language; the program-level emitProgram
 * stitches everything together.
 */
public interface CodeGenerator {

    String name();

    String emitProgram(ModuleAst module);

    // Statements
    String emitTypedVarDecl(TypedVariableDeclarationAndAssignmentData s);
    String emitUntypedVarDecl(UntypedVariableDeclarationAndAssignmentData s);
    String emitVarAssignment(VariableAssignmentData s);
    String emitFunctionImpl(FunctionImplementationData s);
    String emitIfBlock(IfBlockData s);
    String emitForBlock(ForBlockData s);
    String emitWhileBlock(WhileBlockData s);
    String emitFunctionCallStatement(FunctionCallStatementData s);
    String emitReturnStatement(ReturnStatementData s);

    // Expressions and helpers
    String emitExpression(ExpressionData e);
    String emitFunctionCall(FunctionCallData c);
    String emitLambda(LambdaFunctionData l);
    String emitArrayLiteral(ArrayLiteralData a);
    String emitBundleLiteral(BundleLiteralData b);
}
