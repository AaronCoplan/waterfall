package com.aaroncoplan.waterfall.compiler.target

import com.aaroncoplan.waterfall.compiler.statements.ArrayLiteralData
import com.aaroncoplan.waterfall.compiler.statements.BundleLiteralData
import com.aaroncoplan.waterfall.compiler.statements.ExpressionData
import com.aaroncoplan.waterfall.compiler.statements.ForBlockData
import com.aaroncoplan.waterfall.compiler.statements.FunctionCallData
import com.aaroncoplan.waterfall.compiler.statements.FunctionCallStatementData
import com.aaroncoplan.waterfall.compiler.statements.FunctionImplementationData
import com.aaroncoplan.waterfall.compiler.statements.IfBlockData
import com.aaroncoplan.waterfall.compiler.statements.IncrementStatementData
import com.aaroncoplan.waterfall.compiler.statements.LambdaFunctionData
import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.statements.ReturnStatementData
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData
import com.aaroncoplan.waterfall.compiler.statements.UntypedVariableDeclarationAndAssignmentData
import com.aaroncoplan.waterfall.compiler.statements.VariableAssignmentData
import com.aaroncoplan.waterfall.compiler.statements.WhileBlockData

/**
 * Pluggable target-language code generator. One implementation per supported
 * output language. Each method takes a node from the front-end AST and returns
 * its rendered fragment in the target language; the program-level [emitProgram]
 * stitches everything together.
 */
interface CodeGenerator {

    fun name(): String

    fun emitProgram(module: ModuleAst): String

    // Statements
    fun emitTypedVarDecl(s: TypedVariableDeclarationAndAssignmentData): String
    fun emitUntypedVarDecl(s: UntypedVariableDeclarationAndAssignmentData): String
    fun emitVarAssignment(s: VariableAssignmentData): String
    fun emitFunctionImpl(s: FunctionImplementationData): String
    fun emitIfBlock(s: IfBlockData): String
    fun emitForBlock(s: ForBlockData): String
    fun emitWhileBlock(s: WhileBlockData): String
    fun emitFunctionCallStatement(s: FunctionCallStatementData): String
    fun emitReturnStatement(s: ReturnStatementData): String
    fun emitIncrementStatement(s: IncrementStatementData): String

    // Expressions and helpers
    fun emitExpression(e: ExpressionData): String
    fun emitFunctionCall(c: FunctionCallData): String
    fun emitLambda(l: LambdaFunctionData): String
    fun emitArrayLiteral(a: ArrayLiteralData): String
    fun emitBundleLiteral(b: BundleLiteralData): String
}
