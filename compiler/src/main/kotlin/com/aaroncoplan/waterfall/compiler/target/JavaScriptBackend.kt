package com.aaroncoplan.waterfall.compiler.target

import com.aaroncoplan.waterfall.compiler.ir.*
import com.aaroncoplan.waterfall.compiler.statements.StringLiteralText

/**
 * Emits JavaScript. Types are dropped; `isReadonly` maps to `const`, everything
 * else to `let`. for-in → for...of, lambdas → arrow functions.
 * Bundle literals best-guessed to arrays (TODO audit U1). Module::fn(x) → Module.fn(x).
 *
 * §5.5 migration: full IR implementation (real, not a throwaway stub).
 */
class JavaScriptBackend : CodeGenerator {

    private val indentUnit = "    "

    override fun name(): String = "js"

    // ---------------------------------------------------------------------- //
    // Module-level
    // ---------------------------------------------------------------------- //

    override fun emitProgram(module: IrModule): String {
        val sb = StringBuilder()
        sb.append("// module ").append(module.name).append("\n")
        for (v in module.topLevelVariables) {
            sb.append(emitTopLevelVar(v)).append("\n")
        }
        if (module.topLevelVariables.isNotEmpty() && module.functions.isNotEmpty()) {
            sb.append("\n")
        }
        for ((i, fn) in module.functions.withIndex()) {
            sb.append(emitFunction(fn))
            sb.append(if (i < module.functions.size - 1) "\n\n" else "\n")
        }
        return sb.toString()
    }

    /** Top-level variable (IrTopLevelVariable). Same logic as emitTypedVarDecl but for module-level. */
    private fun emitTopLevelVar(v: IrTopLevelVariable): String {
        val keyword = if (v.isReadonly) "const" else "let"
        return "$keyword ${v.name} = ${emitExpression(v.initializer)};"
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

    override fun emitTypedVarDecl(s: IrStatement.TypedVarDecl): String {
        val keyword = if (s.isReadonly) "const" else "let"
        return "$keyword ${s.name} = ${emitExpression(s.initializer)};"
    }

    override fun emitUntypedVarDecl(s: IrStatement.UntypedVarDecl): String {
        val keyword = if (s.isReadonly) "const" else "let"
        return "$keyword ${s.name} = ${emitExpression(s.initializer)};"
    }

    override fun emitVarAssignment(s: IrStatement.VarAssignment): String =
        "${s.name} ${s.op} ${emitExpression(s.value)};"

    override fun emitFunction(s: IrFunction): String {
        val args = s.parameters.joinToString(", ") { it.name }
        if (s.body.isEmpty()) return "function ${s.name}($args) {}"
        val body = indentBlock(s.body)
        return "function ${s.name}($args) {\n$body\n}"
    }

    override fun emitIfBlock(s: IrStatement.IfBlock): String {
        val sb = StringBuilder()
        sb.append("if (").append(emitExpression(s.ifBranch.condition)).append(") {")
        sb.append(blockBody(s.ifBranch.body))
        sb.append("}")
        for (elif in s.elifBranches) {
            sb.append(" else if (").append(emitExpression(elif.condition)).append(") {")
            sb.append(blockBody(elif.body))
            sb.append("}")
        }
        s.elseBody?.let { else_ ->
            sb.append(" else {")
            sb.append(blockBody(else_))
            sb.append("}")
        }
        return sb.toString()
    }

    override fun emitForBlock(s: IrStatement.ForBlock): String =
        "for (const ${s.iteratorName} of ${s.collectionName}) {${blockBody(s.body)}}"

    override fun emitWhileBlock(s: IrStatement.WhileBlock): String =
        "while (${emitExpression(s.condition)}) {${blockBody(s.body)}}"

    override fun emitFunctionCallStatement(s: IrStatement.FunctionCallStatement): String =
        emitFunctionCall(s.call) + ";"

    override fun emitReturnStatement(s: IrStatement.ReturnStatement): String =
        if (s.value == null) "return;" else "return ${emitExpression(s.value)};"

    override fun emitIncrementStatement(s: IrStatement.IncrementStatement): String =
        "${s.name}${s.op};"

    override fun emitReadonlyPromotion(s: IrStatement.ReadonlyPromotion): String =
        error("ReadonlyPromotion is P12-deferred — IR variant must not reach P10 backends")

    // ---------------------------------------------------------------------- //
    // Expression dispatcher
    // ---------------------------------------------------------------------- //

    override fun emitExpression(e: IrExpression): String = when (e) {
        is IrExpression.NullLiteral  -> "null"
        is IrExpression.BoolLiteral  -> e.value.toString()   // "true" / "false"
        is IrExpression.IntLiteral   -> e.literalText
        is IrExpression.DecLiteral   -> e.literalText
        is IrExpression.Identifier   -> e.name               // R5: emit name; ignore Void type
        is IrExpression.StringLiteral ->
            StringLiteralText.escapeFor(StringLiteralText.unescape(e.literalText), '"')!!
        is IrExpression.Lambda       -> emitLambda(e)
        is IrExpression.BundleLiteral-> emitBundleLiteral(e)
        is IrExpression.ArrayLiteral -> emitArrayLiteral(e)
        is IrExpression.FunctionCall -> emitFunctionCall(e)
        is IrExpression.ArrayIndex   -> "${e.target}[${emitExpression(e.index)}]"
        is IrExpression.Cast         -> emitCast(e)
        is IrExpression.BinaryOp     -> {
            val jsOp = when (e.op) {
                "and"    -> "&&"
                "or"     -> "||"
                "equals" -> "==="
                "^"      -> "**"
                else     -> e.op
            }
            "(${emitExpression(e.left)} $jsOp ${emitExpression(e.right)})"
        }
    }

    private fun emitCast(e: IrExpression.Cast): String {
        if (e.targetType is IrType.Array) return emitExpression(e.operand)
        val fn = when (e.targetType) {
            IrType.Int  -> "Math.trunc"
            IrType.Dec  -> "Number"
            IrType.Bool -> "Boolean"
            IrType.Char -> "String"
            else        -> null
        }
        return if (fn == null) emitExpression(e.operand) else "$fn(${emitExpression(e.operand)})"
    }

    override fun emitFunctionCall(c: IrExpression.FunctionCall): String {
        val args = if (c.namedArguments.isNotEmpty()) {
            "{" + c.namedArguments.joinToString(", ") { "${it.first}: ${emitExpression(it.second)}" } + "}"
        } else {
            c.positionalArguments.joinToString(", ") { emitExpression(it) }
        }
        return when (c.kind) {
            IrExpression.FunctionCall.Kind.Local  -> "${c.functionName}($args)"
            IrExpression.FunctionCall.Kind.Module -> "${c.moduleName}.${c.functionName}($args)"
            IrExpression.FunctionCall.Kind.Object ->
                "${c.receiverPath.joinToString(".")}.${c.functionName}($args)"
        }
    }

    override fun emitLambda(l: IrExpression.Lambda): String {
        val argList = l.parameters.joinToString(", ") { it.name }
        val bodyText = if (l.body == null) "{}" else emitFunctionCall(l.body)
        return "($argList) => $bodyText"
    }

    override fun emitArrayLiteral(a: IrExpression.ArrayLiteral): String =
        "[" + a.elements.joinToString(", ") { emitExpression(it) } + "]"

    override fun emitBundleLiteral(b: IrExpression.BundleLiteral): String =
        "[" + b.elements.joinToString(", ") { emitExpression(it) } + "]"

    // ---------------------------------------------------------------------- //
    // Private helpers
    // ---------------------------------------------------------------------- //

    /** Inline block body (no extra indentation); used by if/elif/else/for/while. */
    private fun blockBody(body: List<IrStatement>): String =
        body.joinToString("\n") { emitStatement(it) }

    /** Indented block body; used by function bodies. */
    private fun indentBlock(body: List<IrStatement>): String =
        body.joinToString("\n") { indentUnit + emitStatement(it) }
}
