package com.aaroncoplan.waterfall.compiler.target

import com.aaroncoplan.waterfall.compiler.ir.*
import com.aaroncoplan.waterfall.compiler.statements.StringLiteralText

/**
 * Emits Python 3. Types are dropped (Python is dynamic). Compounds use
 * indentation rather than braces, so each `emit*` method returns its node at
 * "indent level 0" and the [indent] helper adds leading whitespace when placing
 * it inside a parent block.
 *
 * §5.5: full IR-consuming implementation (commit 3 — replaces throwaway stub).
 */
class PythonBackend : CodeGenerator {

    /** True iff any top-level `const`/`imm` var was emitted. Controls the
     *  `from typing import Final` prelude. SA-1: derived from IrModule field. */
    private var usesFinal: Boolean = false

    override fun name(): String = "python"

    override fun emitProgram(module: IrModule): String {
        // Compute usesFinal from IrModule field (SA-1: top-level only per PEP 591).
        usesFinal = module.topLevelVariables.any { it.isReadonly }
        val body = StringBuilder()
        for (v in module.topLevelVariables) {
            body.append(emitTopLevelVar(v)).append("\n")
        }
        if (module.topLevelVariables.isNotEmpty() && module.functions.isNotEmpty()) {
            body.append("\n")
        }
        for ((i, fn) in module.functions.withIndex()) {
            body.append(emitFunction(fn))
            body.append(if (i < module.functions.size - 1) "\n\n\n" else "\n")
        }
        val sb = StringBuilder()
        sb.append("# module ").append(module.name).append("\n")
        if (usesFinal) sb.append("from typing import Final\n")
        sb.append(body)
        return sb.toString()
    }

    private fun emitTopLevelVar(v: IrTopLevelVariable): String {
        return if (v.isReadonly) "${v.name}: Final = ${emitIrExpression(v.initializer)}"
        else "${v.name} = ${emitIrExpression(v.initializer)}"
    }

    // ---------------------------------------------------------------------- //
    // Statement dispatcher (R6)
    // ---------------------------------------------------------------------- //

    private fun emitStatement(s: IrStatement): String = when (s) {
        is IrStatement.TypedVarDecl          -> emitTypedVarDecl(s)
        is IrStatement.UntypedVarDecl        -> emitUntypedVarDecl(s)
        is IrStatement.VarAssignment         -> emitVarAssignment(s)
        is IrStatement.IncrementStatement    -> emitIncrementStatement(s)
        is IrStatement.IfBlock               -> emitIfBlock(s)
        is IrStatement.WhileBlock            -> emitWhileBlock(s)
        is IrStatement.ForBlock              -> emitForBlock(s)
        is IrStatement.ReturnStatement       -> emitReturnStatement(s)
        is IrStatement.FunctionCallStatement -> emitFunctionCallStatement(s)
        is IrStatement.ReadonlyPromotion     -> emitReadonlyPromotion(s)
    }

    // ---------------------------------------------------------------------- //
    // Statement implementations
    // ---------------------------------------------------------------------- //

    override fun emitTypedVarDecl(s: IrStatement.TypedVarDecl): String =
        "${s.name} = ${emitIrExpression(s.initializer)}"  // no Final inside function bodies

    override fun emitUntypedVarDecl(s: IrStatement.UntypedVarDecl): String =
        "${s.name} = ${emitIrExpression(s.initializer)}"

    override fun emitVarAssignment(s: IrStatement.VarAssignment): String =
        "${s.name} ${s.op} ${emitIrExpression(s.value)}"

    override fun emitFunction(s: IrFunction): String {
        val args = s.parameters.joinToString(", ") { it.name }
        val header = "def ${s.name}($args):"
        if (s.body.isEmpty()) return header + "\n" + INDENT_UNIT + "pass"
        return header + "\n" + indent(joinStatements(s.body), 1)
    }

    override fun emitIfBlock(s: IrStatement.IfBlock): String {
        val sb = StringBuilder()
        sb.append("if ").append(emitIrExpression(s.ifBranch.condition)).append(":\n")
        sb.append(indent(bodyOrPass(s.ifBranch.body), 1))
        for (elif in s.elifBranches) {
            sb.append("\n").append("elif ").append(emitIrExpression(elif.condition)).append(":\n")
            sb.append(indent(bodyOrPass(elif.body), 1))
        }
        s.elseBody?.let { else_ ->
            sb.append("\n").append("else:\n")
            sb.append(indent(bodyOrPass(else_), 1))
        }
        return sb.toString()
    }

    override fun emitForBlock(s: IrStatement.ForBlock): String =
        "for ${s.iteratorName} in ${s.collectionName}:\n${indent(bodyOrPass(s.body), 1)}"

    override fun emitWhileBlock(s: IrStatement.WhileBlock): String =
        "while ${emitIrExpression(s.condition)}:\n${indent(bodyOrPass(s.body), 1)}"

    override fun emitFunctionCallStatement(s: IrStatement.FunctionCallStatement): String =
        emitIrFunctionCall(s.call)

    override fun emitReturnStatement(s: IrStatement.ReturnStatement): String =
        if (s.value == null) "return" else "return ${emitIrExpression(s.value)}"

    override fun emitIncrementStatement(s: IrStatement.IncrementStatement): String {
        val delta = if (s.op == "++") "+= 1" else "-= 1"
        return "${s.name} $delta"
    }

    override fun emitReadonlyPromotion(s: IrStatement.ReadonlyPromotion): String =
        error("ReadonlyPromotion is P12-deferred — IR variant must not reach P10 backends")

    // ---------------------------------------------------------------------- //
    // Expression rendering
    // ---------------------------------------------------------------------- //

    private fun emitIrExpression(e: IrExpression): String = when (e) {
        is IrExpression.Identifier   -> e.name                           // R5
        is IrExpression.IntLiteral   -> e.literalText
        is IrExpression.DecLiteral   -> e.literalText
        is IrExpression.NullLiteral  -> "None"
        is IrExpression.BoolLiteral  -> if (e.value) "True" else "False" // Python-specific
        is IrExpression.StringLiteral ->
            StringLiteralText.escapeFor(StringLiteralText.unescape(e.literalText), '"')!!
        is IrExpression.ArrayIndex   -> "${e.target}[${emitIrExpression(e.index)}]"
        is IrExpression.BinaryOp     -> {
            val pyOp = when (e.op) {
                "and" -> "and"; "or" -> "or"; "equals" -> "=="; "^" -> "**"; else -> e.op
            }
            "(${emitIrExpression(e.left)} $pyOp ${emitIrExpression(e.right)})"
        }
        is IrExpression.Cast         -> {
            if (e.targetType is IrType.Array) emitIrExpression(e.operand)
            else {
                val fn = when (e.targetType) {
                    IrType.Int -> "int"; IrType.Dec -> "float"
                    IrType.Bool -> "bool"; IrType.Char -> "str"; else -> null
                }
                if (fn == null) emitIrExpression(e.operand) else "$fn(${emitIrExpression(e.operand)})"
            }
        }
        is IrExpression.FunctionCall  -> emitIrFunctionCall(e)
        is IrExpression.ArrayLiteral  -> "[" + e.elements.joinToString(", ") { emitIrExpression(it) } + "]"
        is IrExpression.BundleLiteral -> "[" + e.elements.joinToString(", ") { emitIrExpression(it) } + "]"
        is IrExpression.Lambda        -> {
            val argList = e.parameters.joinToString(", ") { it.name }
            if (e.body == null) "(lambda $argList: None)"
            else "(lambda $argList: ${emitIrFunctionCall(e.body)})"
        }
    }

    private fun emitIrFunctionCall(c: IrExpression.FunctionCall): String {
        val args = if (c.namedArguments.isNotEmpty()) {
            c.namedArguments.joinToString(", ") { "${it.first}=${emitIrExpression(it.second)}" }
        } else {
            c.positionalArguments.joinToString(", ") { emitIrExpression(it) }
        }
        return when (c.kind) {
            IrExpression.FunctionCall.Kind.Local  -> "${c.functionName}($args)"
            IrExpression.FunctionCall.Kind.Module -> "${c.moduleName}.${c.functionName}($args)"
            IrExpression.FunctionCall.Kind.Object ->
                "${c.receiverPath.joinToString(".")}.${c.functionName}($args)"
        }
    }

    // Interface methods — delegate to IR-aware helpers
    override fun emitExpression(e: IrExpression): String = emitIrExpression(e)
    override fun emitFunctionCall(c: IrExpression.FunctionCall): String = emitIrFunctionCall(c)
    override fun emitLambda(l: IrExpression.Lambda): String = emitIrExpression(l)  // M2: single source
    override fun emitArrayLiteral(a: IrExpression.ArrayLiteral): String =
        "[" + a.elements.joinToString(", ") { emitIrExpression(it) } + "]"
    override fun emitBundleLiteral(b: IrExpression.BundleLiteral): String =
        "[" + b.elements.joinToString(", ") { emitIrExpression(it) } + "]"

    // ---------------------------------------------------------------------- //
    // Private helpers
    // ---------------------------------------------------------------------- //

    private fun joinStatements(body: List<IrStatement>): String =
        body.joinToString("\n") { emitStatement(it) }

    private fun bodyOrPass(body: List<IrStatement>): String =
        if (body.isEmpty()) "pass" else joinStatements(body)

    companion object {
        private const val INDENT_UNIT = "    "

        private fun indent(text: String, level: Int): String {
            if (level <= 0) return text
            val prefix = INDENT_UNIT.repeat(level)
            return text.split("\n").joinToString("\n") { line ->
                if (line.isEmpty()) line else prefix + line
            }
        }
    }
}
