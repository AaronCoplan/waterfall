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
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement

/**
 * The original C-like emitter. Output is byte-identical to what the compiler
 * produced before the backend abstraction landed — see
 * `notes/phase-1-outputs.txt` for the captured baseline.
 */
class LegacyTextBackend : CodeGenerator {

    override fun name(): String = "legacy"

    override fun emitProgram(module: ModuleAst): String {
        val declarations = module.topLevelVariables.joinToString("\n") { it.translate(this) }
        val functions = module.functions.joinToString("\n") { it.translate(this) }
        // Original Container.generate(): headers + "\n\n" + declarations + "\n\n" + functions,
        // with "\n\n" only when the preceding block is non-empty.
        val declarationSpacing = if (module.topLevelVariables.isEmpty()) "" else "\n\n"
        return declarations + declarationSpacing + functions
    }

    override fun emitTypedVarDecl(s: TypedVariableDeclarationAndAssignmentData): String =
        "${s.type} ${s.name} = ${emitExpression(s.value)};"

    override fun emitUntypedVarDecl(s: UntypedVariableDeclarationAndAssignmentData): String =
        "${s.inferredType} ${s.name} = ${emitExpression(s.value)};"

    override fun emitVarAssignment(s: VariableAssignmentData): String =
        "${s.name} ${s.op} ${emitExpression(s.value)};"

    override fun emitFunctionImpl(s: FunctionImplementationData): String {
        val returnType = s.returnType ?: "void"
        val args = s.typedArguments.joinToString(", ") { "${it.firstVal} ${it.secondVal}" }
        val body = s.statements.joinToString("\n") { it.translate(this) }
        return "$returnType ${s.name}($args) {$body}"
    }

    override fun emitIfBlock(s: IfBlockData): String {
        val sb = StringBuilder()
        sb.append("if (").append(emitExpression(s.ifBranch.condition)).append(") {")
        sb.append(joinBody(s.ifBranch.body))
        sb.append("}")
        for (elif in s.elifBranches) {
            sb.append(" else if (").append(emitExpression(elif.condition)).append(") {")
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

    override fun emitForBlock(s: ForBlockData): String =
        "for (auto ${s.iteratorName} : ${s.collectionName}) {${joinBody(s.body)}}"

    override fun emitWhileBlock(s: WhileBlockData): String =
        "while (${emitExpression(s.condition)}) {${joinBody(s.body)}}"

    override fun emitFunctionCallStatement(s: FunctionCallStatementData): String =
        emitFunctionCall(s.call) + ";"

    override fun emitReturnStatement(s: ReturnStatementData): String =
        if (s.value == null) "return;" else "return ${emitExpression(s.value)};"

    override fun emitIncrementStatement(s: IncrementStatementData): String =
        "${s.name}${s.op};"

    override fun emitExpression(e: ExpressionData): String = when (e.kind) {
        ExpressionData.Kind.NULL_LITERAL -> "NULL"
        ExpressionData.Kind.BOOL_LITERAL,
        ExpressionData.Kind.INT_LITERAL,
        ExpressionData.Kind.DEC_LITERAL,
        ExpressionData.Kind.IDENTIFIER,
        ExpressionData.Kind.STRING_LITERAL -> e.literalText!!
        ExpressionData.Kind.LAMBDA -> emitLambda(e.lambda!!)
        ExpressionData.Kind.BUNDLE -> emitBundleLiteral(e.bundle!!)
        ExpressionData.Kind.ARRAY -> emitArrayLiteral(e.array!!)
        ExpressionData.Kind.FUNCTION_CALL -> emitFunctionCall(e.functionCall!!)
        ExpressionData.Kind.BINARY_OP ->
            // Pass through source operator text verbatim.
            "(${emitExpression(e.left!!)} ${e.op} ${emitExpression(e.right!!)})"
        ExpressionData.Kind.ARRAY_INDEX ->
            "${e.arrayIndex!!.target}[${emitExpression(e.arrayIndex.index)}]"
        ExpressionData.Kind.CAST ->
            "(${emitExpression(e.castOperand!!)} castas ${e.castTargetType})"
    }

    override fun emitFunctionCall(c: FunctionCallData): String {
        val args =
            if (c.namedArguments.isEmpty()) c.positionalArguments.joinToString(", ") { emitExpression(it) }
            else c.namedArguments.joinToString(", ") { "${it.firstVal}=${emitExpression(it.secondVal)}" }
        return when (c.kind) {
            FunctionCallData.Kind.LOCAL  -> "${c.functionName}($args)"
            FunctionCallData.Kind.MODULE -> "${c.moduleName}_${c.functionName}($args)"
            FunctionCallData.Kind.OBJECT -> "${c.receiverPath.joinToString(".")}.${c.functionName}($args)"
        }
    }

    override fun emitLambda(l: LambdaFunctionData): String {
        val argList = l.typedArguments.joinToString(", ") { "${it.firstVal} ${it.secondVal}" }
        val bodyText = if (l.body == null) "{}" else emitFunctionCall(l.body)
        return "($argList) ==> $bodyText"
    }

    override fun emitArrayLiteral(a: ArrayLiteralData): String =
        "[" + a.elements.joinToString(", ") { emitExpression(it) } + "]"

    override fun emitBundleLiteral(b: BundleLiteralData): String =
        "|" + b.elements.joinToString(", ") { emitExpression(it) } + "|"

    private fun joinBody(body: List<TranslatableStatement>): String =
        body.joinToString("\n") { it.translate(this) }
}
