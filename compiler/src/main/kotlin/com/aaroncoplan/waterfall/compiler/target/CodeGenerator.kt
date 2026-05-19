package com.aaroncoplan.waterfall.compiler.target

import com.aaroncoplan.waterfall.compiler.ir.*

/**
 * Pluggable target-language code generator. Every method takes an IR node and
 * returns its rendered form in the target language. Backends never see the
 * source-form `*Data` classes anymore (§5.5 migration).
 *
 * [emitReadonlyPromotion] throws in all P10 backends — the IR variant is
 * P12-deferred and must not reach the backend. (Aaron D3=throw decision.)
 *
 * Each backend SHOULD implement a private `emitStatement(s: IrStatement): String`
 * dispatcher (per §3.9 R6 note) that routes to the appropriate `emit*` method.
 * This replaces the old `it.translate(this)` pattern and prevents a future backend
 * from accidentally bypassing the dispatcher. Consider promoting to a default
 * interface method at §5.6 cleanup time if a fourth backend arrives. (M5)
 */
interface CodeGenerator {

    fun name(): String

    fun emitProgram(module: IrModule): String

    // Statements
    fun emitTypedVarDecl(s: IrStatement.TypedVarDecl): String
    fun emitUntypedVarDecl(s: IrStatement.UntypedVarDecl): String
    fun emitVarAssignment(s: IrStatement.VarAssignment): String
    fun emitFunction(s: IrFunction): String                              // M3: was emitFunctionImpl
    fun emitIfBlock(s: IrStatement.IfBlock): String
    fun emitForBlock(s: IrStatement.ForBlock): String
    fun emitWhileBlock(s: IrStatement.WhileBlock): String
    fun emitFunctionCallStatement(s: IrStatement.FunctionCallStatement): String
    fun emitReturnStatement(s: IrStatement.ReturnStatement): String
    fun emitIncrementStatement(s: IrStatement.IncrementStatement): String
    fun emitReadonlyPromotion(s: IrStatement.ReadonlyPromotion): String  // always throws in P10

    // Expressions and helpers
    fun emitExpression(e: IrExpression): String
    fun emitFunctionCall(c: IrExpression.FunctionCall): String
    fun emitLambda(l: IrExpression.Lambda): String
    fun emitArrayLiteral(a: IrExpression.ArrayLiteral): String
    fun emitBundleLiteral(b: IrExpression.BundleLiteral): String
}
