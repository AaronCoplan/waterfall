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
 * Emits JavaScript. Types are dropped; const/imm modifiers map to `const`,
 * everything else to `let`. for-in -> for...of, lambdas -> arrow functions.
 * Bundle literals are best-guessed to arrays (TODO(audit) flagged) and
 * Module::fn(x) -> Module.fn(x).
 */
class JavaScriptBackend : CodeGenerator {

    private val indentUnit = "    "

    override fun name(): String = "js"

    override fun emitProgram(module: ModuleAst): String {
        val sb = StringBuilder()
        sb.append("// module ").append(module.name).append("\n")
        for (v in module.topLevelVariables) {
            sb.append(emitTypedVarDecl(v)).append("\n")
        }
        if (module.topLevelVariables.isNotEmpty() && module.functions.isNotEmpty()) {
            sb.append("\n")
        }
        for ((i, fn) in module.functions.withIndex()) {
            sb.append(emitFunctionImpl(fn))
            sb.append(if (i < module.functions.size - 1) "\n\n" else "\n")
        }
        return sb.toString()
    }

    override fun emitTypedVarDecl(s: TypedVariableDeclarationAndAssignmentData): String {
        val keyword = if (s.isImmutable()) "const" else "let"
        return "$keyword ${s.name} = ${emitExpression(s.value)};"
    }

    override fun emitUntypedVarDecl(s: UntypedVariableDeclarationAndAssignmentData): String {
        val keyword = if (s.isImmutable()) "const" else "let"
        return "$keyword ${s.name} = ${emitExpression(s.value)};"
    }

    override fun emitVarAssignment(s: VariableAssignmentData): String =
        "${s.name} ${s.op} ${emitExpression(s.value)};"

    override fun emitFunctionImpl(s: FunctionImplementationData): String {
        val args = s.typedArguments.joinToString(", ") { it.secondVal }
        if (s.statements.isEmpty()) return "function ${s.name}($args) {}"
        val body = indentBlock(s.statements)
        return "function ${s.name}($args) {\n$body\n}"
    }

    override fun emitIfBlock(s: IfBlockData): String {
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

    override fun emitForBlock(s: ForBlockData): String =
        "for (const ${s.iteratorName} of ${s.collectionName}) {${blockBody(s.body)}}"

    override fun emitWhileBlock(s: WhileBlockData): String =
        "while (${emitExpression(s.condition)}) {${blockBody(s.body)}}"

    override fun emitFunctionCallStatement(s: FunctionCallStatementData): String =
        emitFunctionCall(s.call) + ";"

    override fun emitReturnStatement(s: ReturnStatementData): String =
        if (s.value == null) "return;" else "return ${emitExpression(s.value)};"

    override fun emitIncrementStatement(s: IncrementStatementData): String =
        "${s.name}${s.op};"

    override fun emitExpression(e: ExpressionData): String = when (e.kind) {
        ExpressionData.Kind.NULL_LITERAL -> "null"
        ExpressionData.Kind.BOOL_LITERAL,
        ExpressionData.Kind.INT_LITERAL,
        ExpressionData.Kind.DEC_LITERAL,
        ExpressionData.Kind.IDENTIFIER -> e.literalText!!
        ExpressionData.Kind.STRING_LITERAL ->
            // Decode the source's backticked form and re-emit as a JS double-quoted
            // string. (Template literals would also be valid but invite ${...}
            // interpolation surprises; plain quoted strings are simpler.)
            StringLiteralText.escapeFor(StringLiteralText.unescape(e.literalText), '"')!!
        ExpressionData.Kind.LAMBDA -> emitLambda(e.lambda!!)
        ExpressionData.Kind.BUNDLE -> emitBundleLiteral(e.bundle!!)
        ExpressionData.Kind.ARRAY -> emitArrayLiteral(e.array!!)
        ExpressionData.Kind.FUNCTION_CALL -> emitFunctionCall(e.functionCall!!)
        ExpressionData.Kind.ARRAY_INDEX ->
            "${e.arrayIndex!!.target}[${emitExpression(e.arrayIndex.index)}]"
        ExpressionData.Kind.CAST -> {
            // Array-typed casts have no JS conversion — arrays are untyped.
            // Emit the operand untouched.
            if (e.castTargetType!!.endsWith("[]")) {
                emitExpression(e.castOperand!!)
            } else {
                val fn = when (e.castTargetType) {
                    "int"  -> "Math.trunc"  // best-guess: truncate to integer
                    "dec"  -> "Number"
                    "bool" -> "Boolean"
                    "char" -> "String"
                    else   -> null
                }
                if (fn == null) emitExpression(e.castOperand!!)
                else "$fn(${emitExpression(e.castOperand!!)})"
            }
        }
        ExpressionData.Kind.BINARY_OP -> {
            val jsOp = when (e.op) {
                "and" -> "&&"
                "or"  -> "||"
                "equals" -> "==="
                "^"   -> "**"  // Waterfall uses ^ for power; JS uses **
                else  -> e.op  // +, -, *, /, %, <, >, <=, >=
            }
            "(${emitExpression(e.left!!)} $jsOp ${emitExpression(e.right!!)})"
        }
    }

    override fun emitFunctionCall(c: FunctionCallData): String {
        val args = if (c.namedArguments.isNotEmpty()) {
            // TODO(audit): named args -> single object literal. The callee must be written
            // to destructure { name: value, ... } as its first parameter.
            "{" + c.namedArguments.joinToString(", ") { "${it.firstVal}: ${emitExpression(it.secondVal)}" } + "}"
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
        val bodyText = if (l.body == null) "{}" else emitFunctionCall(l.body)
        return "($argList) => $bodyText"
    }

    override fun emitArrayLiteral(a: ArrayLiteralData): String =
        "[" + a.elements.joinToString(", ") { emitExpression(it) } + "]"

    override fun emitBundleLiteral(b: BundleLiteralData): String =
        // TODO(audit): bundle semantics aren't defined yet; emitting as a JS array.
        "[" + b.elements.joinToString(", ") { emitExpression(it) } + "]"

    private fun blockBody(body: List<TranslatableStatement>): String =
        body.joinToString("\n") { it.translate(this) }

    private fun indentBlock(body: List<TranslatableStatement>): String =
        body.joinToString("\n") { indentUnit + it.translate(this) }
}
