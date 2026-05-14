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
import com.aaroncoplan.waterfall.compiler.statements.StringLiteralText
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData
import com.aaroncoplan.waterfall.compiler.statements.UntypedVariableDeclarationAndAssignmentData
import com.aaroncoplan.waterfall.compiler.statements.VariableAssignmentData
import com.aaroncoplan.waterfall.compiler.statements.WhileBlockData
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement

/**
 * Emits Python 3. Types are dropped (Python is dynamic). Compounds use
 * indentation rather than braces, so each `emit*` method returns its node at
 * "indent level 0" and the [indent] helper adds leading whitespace when placing
 * it inside a parent block.
 */
class PythonBackend : CodeGenerator {

    /** Whether any const/imm decl was emitted; controls the `from typing import Final` prelude. */
    private var usesFinal: Boolean = false

    override fun name(): String = "python"

    override fun emitProgram(module: ModuleAst): String {
        // Render the body first so usesFinal reflects whether any const/imm got emitted.
        usesFinal = false
        val body = StringBuilder()
        for (v in module.topLevelVariables) {
            body.append(emitTypedVarDecl(v)).append("\n")
        }
        if (module.topLevelVariables.isNotEmpty() && module.functions.isNotEmpty()) {
            body.append("\n")
        }
        for ((i, fn) in module.functions.withIndex()) {
            body.append(emitFunctionImpl(fn))
            body.append(if (i < module.functions.size - 1) "\n\n\n" else "\n")
        }

        val sb = StringBuilder()
        sb.append("# module ").append(module.name).append("\n")
        if (usesFinal) sb.append("from typing import Final\n")
        sb.append(body)
        return sb.toString()
    }

    override fun emitTypedVarDecl(s: TypedVariableDeclarationAndAssignmentData): String {
        if (s.isImmutable()) {
            usesFinal = true
            return "${s.name}: Final = ${emitExpression(s.value)}"
        }
        return "${s.name} = ${emitExpression(s.value)}"
    }

    override fun emitUntypedVarDecl(s: UntypedVariableDeclarationAndAssignmentData): String {
        if (s.isImmutable()) {
            usesFinal = true
            return "${s.name}: Final = ${emitExpression(s.value)}"
        }
        return "${s.name} = ${emitExpression(s.value)}"
    }

    override fun emitVarAssignment(s: VariableAssignmentData): String =
        "${s.name} ${s.op} ${emitExpression(s.value)}"

    override fun emitFunctionImpl(s: FunctionImplementationData): String {
        val args = s.typedArguments.joinToString(", ") { it.secondVal }
        val header = "def ${s.name}($args):"
        if (s.statements.isEmpty()) return header + "\n" + INDENT_UNIT + "pass"
        return header + "\n" + indent(joinStatements(s.statements), 1)
    }

    override fun emitIfBlock(s: IfBlockData): String {
        val sb = StringBuilder()
        sb.append("if ").append(emitExpression(s.ifBranch.condition)).append(":\n")
        sb.append(indent(bodyOrPass(s.ifBranch.body), 1))
        for (elif in s.elifBranches) {
            sb.append("\n").append("elif ").append(emitExpression(elif.condition)).append(":\n")
            sb.append(indent(bodyOrPass(elif.body), 1))
        }
        s.elseBody?.let { else_ ->
            sb.append("\n").append("else:\n")
            sb.append(indent(bodyOrPass(else_), 1))
        }
        return sb.toString()
    }

    override fun emitForBlock(s: ForBlockData): String =
        "for ${s.iteratorName} in ${s.collectionName}:\n${indent(bodyOrPass(s.body), 1)}"

    override fun emitWhileBlock(s: WhileBlockData): String =
        "while ${emitExpression(s.condition)}:\n${indent(bodyOrPass(s.body), 1)}"

    override fun emitFunctionCallStatement(s: FunctionCallStatementData): String =
        emitFunctionCall(s.call)

    override fun emitReturnStatement(s: ReturnStatementData): String =
        if (s.value == null) "return" else "return ${emitExpression(s.value)}"

    override fun emitIncrementStatement(s: IncrementStatementData): String {
        // Python lacks ++/--; lower to augmented assignment.
        val delta = if (s.op == "++") "+= 1" else "-= 1"
        return "${s.name} $delta"
    }

    override fun emitExpression(e: ExpressionData): String = when (e.kind) {
        ExpressionData.Kind.NULL_LITERAL -> "None"
        ExpressionData.Kind.BOOL_LITERAL -> if (e.literalText == "true") "True" else "False"
        ExpressionData.Kind.INT_LITERAL,
        ExpressionData.Kind.DEC_LITERAL,
        ExpressionData.Kind.IDENTIFIER -> e.literalText!!
        ExpressionData.Kind.STRING_LITERAL ->
            StringLiteralText.escapeFor(StringLiteralText.unescape(e.literalText), '"')!!
        ExpressionData.Kind.LAMBDA -> emitLambda(e.lambda!!)
        ExpressionData.Kind.BUNDLE -> emitBundleLiteral(e.bundle!!)
        ExpressionData.Kind.ARRAY -> emitArrayLiteral(e.array!!)
        ExpressionData.Kind.FUNCTION_CALL -> emitFunctionCall(e.functionCall!!)
        ExpressionData.Kind.ARRAY_INDEX ->
            "${e.arrayIndex!!.target}[${emitExpression(e.arrayIndex.index)}]"
        ExpressionData.Kind.CAST -> {
            // Array-typed casts (`castas int[]`) have no Python conversion — Python lists
            // are dynamically typed. Emit the operand untouched.
            if (e.castTargetType!!.endsWith("[]")) {
                emitExpression(e.castOperand!!)
            } else {
                val fn = when (e.castTargetType) {
                    "int" -> "int"
                    "dec" -> "float"
                    "bool" -> "bool"
                    "char" -> "str"
                    else -> null
                }
                if (fn == null) emitExpression(e.castOperand!!)
                else "$fn(${emitExpression(e.castOperand!!)})"
            }
        }
        ExpressionData.Kind.BINARY_OP -> {
            val pyOp = when (e.op) {
                "and" -> "and"
                "or" -> "or"
                "equals" -> "=="
                "^" -> "**"  // Waterfall ^ is power; Python uses **
                else -> e.op  // +, -, *, /, %, <, >, <=, >=
            }
            "(${emitExpression(e.left!!)} $pyOp ${emitExpression(e.right!!)})"
        }
    }

    override fun emitFunctionCall(c: FunctionCallData): String {
        val args = if (c.namedArguments.isNotEmpty()) {
            c.namedArguments.joinToString(", ") { "${it.firstVal}=${emitExpression(it.secondVal)}" }
        } else {
            c.positionalArguments.joinToString(", ") { emitExpression(it) }
        }
        return when (c.kind) {
            FunctionCallData.Kind.LOCAL  -> "${c.functionName}($args)"
            FunctionCallData.Kind.MODULE -> "${c.moduleName}.${c.functionName}($args)"
            FunctionCallData.Kind.OBJECT -> "${c.receiverPath.joinToString(".")}.${c.functionName}($args)"
        }
    }

    override fun emitLambda(l: LambdaFunctionData): String {
        val argList = l.typedArguments.joinToString(", ") { it.secondVal }
        return if (l.body == null) {
            // Empty lambda body. Python can't have a statement-less lambda; use None.
            "(lambda $argList: None)"
        } else {
            "(lambda $argList: ${emitFunctionCall(l.body)})"
        }
    }

    override fun emitArrayLiteral(a: ArrayLiteralData): String =
        "[" + a.elements.joinToString(", ") { emitExpression(it) } + "]"

    override fun emitBundleLiteral(b: BundleLiteralData): String =
        // TODO(audit): bundle semantics aren't defined yet; emitting as a Python list.
        "[" + b.elements.joinToString(", ") { emitExpression(it) } + "]"

    private fun joinStatements(body: List<TranslatableStatement>): String =
        body.joinToString("\n") { it.translate(this) }

    private fun bodyOrPass(body: List<TranslatableStatement>): String =
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
