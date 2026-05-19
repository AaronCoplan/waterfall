package com.aaroncoplan.waterfall.compiler.target

import com.aaroncoplan.waterfall.compiler.ir.*
import com.aaroncoplan.waterfall.compiler.statements.StringLiteralText
import java.util.TreeSet

/**
 * Emits C99. Forces the type system to be real: each Waterfall primitive maps to a C
 * type (`int`→`int`, `dec`→`double`, `bool`→`bool`, `char`→`char`). Many constructs
 * (lambdas, bundles, for-in over an unknown collection, object method calls) don't
 * have a direct C equivalent and emit a best-guess placeholder with a
 * `/* TODO(audit) ... */` comment — these are the spots that surface real follow-up work.
 *
 * §5.5 THROWAWAY STUB: [emitProgram] delegates to [lowerThenEmit] which
 * translates IrModule fields using the same logic as the old *Data-walking
 * implementation. The real IR-consuming implementation lands in commit 4
 * and replaces this entire file. Marked explicitly so the reviewer knows
 * this code will be deleted.
 */
class CBackend : CodeGenerator {

    /**
     * Headers requested by the body of the program. Populated during emission, then
     * emitted at the top of [emitProgram] after the module-name comment.
     */
    private var requiredHeaders: TreeSet<String> = TreeSet()

    override fun name(): String = "c"

    // ---------------------------------------------------------------------- //
    // THROWAWAY: replaced by proper impl in commit 4
    // ---------------------------------------------------------------------- //

    override fun emitProgram(module: IrModule): String {
        requiredHeaders = TreeSet()
        val body = StringBuilder()
        for (v in module.topLevelVariables) {
            body.append(emitTopLevelVar(v)).append("\n")
        }
        if (module.topLevelVariables.isNotEmpty() && module.functions.isNotEmpty()) {
            body.append("\n")
        }
        for ((i, fn) in module.functions.withIndex()) {
            body.append(emitFunction(fn))
            body.append(if (i < module.functions.size - 1) "\n\n" else "\n")
        }
        val sb = StringBuilder()
        sb.append("// module ").append(module.name).append("\n")
        for (header in requiredHeaders) {
            sb.append("#include ").append(header).append("\n")
        }
        if (requiredHeaders.isNotEmpty()) sb.append("\n")
        sb.append(body)
        return sb.toString()
    }

    /** Top-level variable. Emitted directly in [emitProgram]. */
    private fun emitTopLevelVar(v: IrTopLevelVariable): String {
        val prefix = if (v.isReadonly) "const " else ""
        return "$prefix${irTypeToCType(v.type)} ${v.name} = ${emitIrExpression(v.initializer)};"
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
    // Type mapping (R3: use IR's pre-computed types, not string re-derivation)
    // ---------------------------------------------------------------------- //

    /**
     * Map an [IrType] to its C equivalent. Uses IR's pre-computed type (§5.5 R3).
     * Array types map to pointers (`T*`). Unknown types pass through.
     */
    private fun irTypeToCType(t: IrType): String = when (t) {
        IrType.Int  -> "int"
        IrType.Dec  -> "double"
        IrType.Bool -> { requiredHeaders.add("<stdbool.h>"); "bool" }
        IrType.Char -> "char"
        IrType.Void -> "void"
        is IrType.Array -> "${irTypeToCType(t.element)} *"
        else -> t.render()
    }

    // ---------------------------------------------------------------------- //
    // Statement implementations
    // ---------------------------------------------------------------------- //

    override fun emitTypedVarDecl(s: IrStatement.TypedVarDecl): String {
        val prefix = if (s.isReadonly) "const " else ""
        return "$prefix${irTypeToCType(s.type)} ${s.name} = ${emitIrExpression(s.initializer)};"
    }

    override fun emitUntypedVarDecl(s: IrStatement.UntypedVarDecl): String {
        val prefix = if (s.isReadonly) "const " else ""
        return "$prefix${irTypeToCType(s.inferredType)} ${s.name} = ${emitIrExpression(s.initializer)};"
    }

    override fun emitVarAssignment(s: IrStatement.VarAssignment): String =
        "${s.name} ${s.op} ${emitIrExpression(s.value)};"

    override fun emitFunction(s: IrFunction): String {
        val returnType = irTypeToCType(s.returnType)
        val args = s.parameters.joinToString(", ") { "${irTypeToCType(it.type)} ${it.name}" }
        val argsOrVoid = if (args.isEmpty()) "void" else args
        if (s.body.isEmpty()) return "$returnType ${s.name}($argsOrVoid) {}"
        val body = s.body.joinToString("\n") { "    " + emitStatement(it) }
        return "$returnType ${s.name}($argsOrVoid) {\n$body\n}"
    }

    override fun emitIfBlock(s: IrStatement.IfBlock): String {
        val sb = StringBuilder()
        sb.append("if (").append(emitIrExpression(s.ifBranch.condition)).append(") {")
        sb.append(joinBody(s.ifBranch.body))
        sb.append("}")
        for (elif in s.elifBranches) {
            sb.append(" else if (").append(emitIrExpression(elif.condition)).append(") {")
            sb.append(joinBody(elif.body))
            sb.append("}")
        }
        s.elseBody?.let { else_ ->
            sb.append(" else {")
            sb.append(joinBody(else_))
            sb.append("}")
        }
        return sb.toString()
    }

    override fun emitWhileBlock(s: IrStatement.WhileBlock): String =
        "while (${emitIrExpression(s.condition)}) {${joinBody(s.body)}}"

    override fun emitForBlock(s: IrStatement.ForBlock): String {
        return "for (int ${s.iteratorName} = 0; ${s.iteratorName} < 0; ${s.iteratorName}++) " +
                "/* TODO(audit): for-in over ${s.collectionName} */ {${joinBody(s.body)}}"
    }

    override fun emitFunctionCallStatement(s: IrStatement.FunctionCallStatement): String =
        emitIrFunctionCall(s.call) + ";"

    override fun emitReturnStatement(s: IrStatement.ReturnStatement): String =
        if (s.value == null) "return;" else "return ${emitIrExpression(s.value)};"

    override fun emitIncrementStatement(s: IrStatement.IncrementStatement): String =
        "${s.name}${s.op};"

    override fun emitReadonlyPromotion(s: IrStatement.ReadonlyPromotion): String =
        error("ReadonlyPromotion is P12-deferred — IR variant must not reach P10 backends")

    // ---------------------------------------------------------------------- //
    // Expression rendering — inlined (C-specific)
    // ---------------------------------------------------------------------- //

    private fun emitIrExpression(e: IrExpression): String = when (e) {
        is IrExpression.Identifier   -> e.name                           // R5
        is IrExpression.IntLiteral   -> e.literalText
        is IrExpression.DecLiteral   -> e.literalText
        is IrExpression.NullLiteral  -> "NULL"
        is IrExpression.BoolLiteral  -> {
            requiredHeaders.add("<stdbool.h>")
            e.value.toString()   // "true" / "false" (from <stdbool.h>)
        }
        is IrExpression.StringLiteral ->
            StringLiteralText.escapeFor(StringLiteralText.unescape(e.literalText), '"')!!
        is IrExpression.ArrayIndex   -> "${e.target}[${emitIrExpression(e.index)}]"
        is IrExpression.BinaryOp     -> {
            if (e.op == "^") {
                requiredHeaders.add("<math.h>")
                "pow(${emitIrExpression(e.left)}, ${emitIrExpression(e.right)})"
            } else {
                val cOp = when (e.op) {
                    "and" -> "&&"; "or" -> "||"; "equals" -> "=="; else -> e.op
                }
                "(${emitIrExpression(e.left)} $cOp ${emitIrExpression(e.right)})"
            }
        }
        is IrExpression.Cast         ->
            "((${irTypeToCType(e.targetType)})(${emitIrExpression(e.operand)}))"
        is IrExpression.FunctionCall  -> emitIrFunctionCall(e)
        is IrExpression.ArrayLiteral  -> {
            // R3: use IR's pre-computed element type instead of re-deriving from source
            val elemType = (e.type as? IrType.Array)?.element?.let { irTypeToCType(it) } ?: "int"
            val body = e.elements.joinToString(", ") { emitIrExpression(it) }
            "($elemType[]){$body}"
        }
        is IrExpression.BundleLiteral -> "/* TODO(audit): bundle */ {0}"
        is IrExpression.Lambda        -> "/* TODO(audit): lambda */ NULL"
    }

    private fun emitIrFunctionCall(c: IrExpression.FunctionCall): String {
        val args = if (c.namedArguments.isEmpty()) {
            c.positionalArguments.joinToString(", ") { emitIrExpression(it) }
        } else {
            c.namedArguments.joinToString(", ") { emitIrExpression(it.second) }
        }
        return when (c.kind) {
            IrExpression.FunctionCall.Kind.Local  -> "${c.functionName}($args)"
            IrExpression.FunctionCall.Kind.Module -> "${c.moduleName}_${c.functionName}($args)"
            IrExpression.FunctionCall.Kind.Object -> {
                val receiver = c.receiverPath.joinToString(".")
                val all = if (args.isEmpty()) "&$receiver" else "&$receiver, $args"
                "${receiver}_${c.functionName}($all)"
            }
        }
    }

    // Interface method delegations
    override fun emitExpression(e: IrExpression): String = emitIrExpression(e)
    override fun emitFunctionCall(c: IrExpression.FunctionCall): String = emitIrFunctionCall(c)
    override fun emitLambda(l: IrExpression.Lambda): String = "/* TODO(audit): lambda */ NULL"
    override fun emitArrayLiteral(a: IrExpression.ArrayLiteral): String = emitIrExpression(a)
    override fun emitBundleLiteral(b: IrExpression.BundleLiteral): String = "/* TODO(audit): bundle */ {0}"

    // ---------------------------------------------------------------------- //
    // Private helpers
    // ---------------------------------------------------------------------- //

    private fun joinBody(body: List<IrStatement>): String =
        body.joinToString("\n") { emitStatement(it) }
}
