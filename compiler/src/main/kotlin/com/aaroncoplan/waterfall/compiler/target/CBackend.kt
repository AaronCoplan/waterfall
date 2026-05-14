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
import com.aaroncoplan.waterfall.compiler.typesystem.PrimitiveTypes
import java.util.TreeSet

/**
 * Emits C99. Forces the type system to be real: each Waterfall primitive maps to a C
 * type (`int`→`int`, `dec`→`double`, `bool`→`bool`, `char`→`char`). Many constructs
 * (lambdas, bundles, for-in over an unknown collection, object method calls) don't
 * have a direct C equivalent and emit a best-guess placeholder with a
 * `/* TODO(audit) ... */` comment — these are the spots that surface real follow-up work.
 */
class CBackend : CodeGenerator {

    /**
     * Headers requested by the body of the program. Populated during emission, then
     * emitted at the top of [emitProgram] after the module-name comment.
     */
    private var requiredHeaders: TreeSet<String> = TreeSet()

    override fun name(): String = "c"

    override fun emitProgram(module: ModuleAst): String {
        // Render the body first so the header set is populated by the time we serialize.
        requiredHeaders = TreeSet()
        val body = StringBuilder()
        for (v in module.topLevelVariables) {
            body.append(emitTypedVarDecl(v)).append("\n")
        }
        if (module.topLevelVariables.isNotEmpty() && module.functions.isNotEmpty()) {
            body.append("\n")
        }
        for ((i, fn) in module.functions.withIndex()) {
            body.append(emitFunctionImpl(fn))
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

    /**
     * Map a Waterfall type name to its C equivalent and request any header it needs.
     * Array types (`T[]`) map to pointers (`T*`). Unknown types pass through;
     * gcc -fsyntax-only will reject them.
     */
    private fun cType(wfType: String?): String {
        if (wfType == null) return "void"
        if (PrimitiveTypes.isArray(wfType)) {
            return "${cType(PrimitiveTypes.elementType(wfType))} *"
        }
        return when (wfType) {
            PrimitiveTypes.INT  -> "int"
            PrimitiveTypes.DEC  -> "double"
            PrimitiveTypes.BOOL -> { requiredHeaders.add("<stdbool.h>"); "bool" }
            PrimitiveTypes.CHAR -> "char"
            else -> wfType
        }
    }

    override fun emitTypedVarDecl(s: TypedVariableDeclarationAndAssignmentData): String {
        val prefix = if (s.isImmutable()) "const " else ""
        return "$prefix${cType(s.type)} ${s.name} = ${emitExpression(s.value)};"
    }

    override fun emitUntypedVarDecl(s: UntypedVariableDeclarationAndAssignmentData): String {
        val prefix = if (s.isImmutable()) "const " else ""
        return "$prefix${cType(s.inferredType)} ${s.name} = ${emitExpression(s.value)};"
    }

    override fun emitVarAssignment(s: VariableAssignmentData): String =
        "${s.name} ${s.op} ${emitExpression(s.value)};"

    override fun emitFunctionImpl(s: FunctionImplementationData): String {
        val returnType = cType(s.returnType)
        val args = s.typedArguments.joinToString(", ") { "${cType(it.firstVal)} ${it.secondVal}" }
        val argsOrVoid = if (args.isEmpty()) "void" else args
        if (s.statements.isEmpty()) return "$returnType ${s.name}($argsOrVoid) {}"
        val body = s.statements.joinToString("\n") { "    " + it.translate(this) }
        return "$returnType ${s.name}($argsOrVoid) {\n$body\n}"
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

    override fun emitWhileBlock(s: WhileBlockData): String =
        "while (${emitExpression(s.condition)}) {${joinBody(s.body)}}"

    override fun emitForBlock(s: ForBlockData): String {
        // TODO(audit): C has no for-in. Emitting a zero-iteration loop that still declares
        // the iterator so the body's reference type-checks. Real lowering needs collection
        // semantics (pointer + length, iterator protocol, etc.).
        return "for (int ${s.iteratorName} = 0; ${s.iteratorName} < 0; ${s.iteratorName}++) " +
                "/* TODO(audit): for-in over ${s.collectionName} */ {${joinBody(s.body)}}"
    }

    override fun emitFunctionCallStatement(s: FunctionCallStatementData): String =
        emitFunctionCall(s.call) + ";"

    override fun emitReturnStatement(s: ReturnStatementData): String =
        if (s.value == null) "return;" else "return ${emitExpression(s.value)};"

    override fun emitIncrementStatement(s: IncrementStatementData): String =
        "${s.name}${s.op};"

    override fun emitExpression(e: ExpressionData): String = when (e.kind) {
        ExpressionData.Kind.NULL_LITERAL -> "NULL"
        ExpressionData.Kind.BOOL_LITERAL -> {
            requiredHeaders.add("<stdbool.h>")
            e.literalText!!
        }
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
        ExpressionData.Kind.CAST ->
            "((${cType(e.castTargetType)})(${emitExpression(e.castOperand!!)}))"
        ExpressionData.Kind.BINARY_OP -> {
            if (e.op == "^") {
                // C: ^ is XOR, not power. README defines ^ as power, so lower to pow().
                // TODO(audit): also needs `-lm` at link time.
                requiredHeaders.add("<math.h>")
                "pow(${emitExpression(e.left!!)}, ${emitExpression(e.right!!)})"
            } else {
                val cOp = when (e.op) {
                    "and" -> "&&"
                    "or" -> "||"
                    "equals" -> "=="
                    else -> e.op  // +, -, *, /, %, <, >, <=, >=
                }
                "(${emitExpression(e.left!!)} $cOp ${emitExpression(e.right!!)})"
            }
        }
    }

    override fun emitFunctionCall(c: FunctionCallData): String {
        val args = if (c.namedArguments.isEmpty()) {
            c.positionalArguments.joinToString(", ") { emitExpression(it) }
        } else {
            // TODO(audit): named args dropped; C has no native named-arg support.
            c.namedArguments.joinToString(", ") { emitExpression(it.secondVal) }
        }
        return when (c.kind) {
            FunctionCallData.Kind.LOCAL  -> "${c.functionName}($args)"
            // TODO(audit): Mod::fn -> Mod_fn mangle. Real solution: header-per-module.
            FunctionCallData.Kind.MODULE -> "${c.moduleName}_${c.functionName}($args)"
            FunctionCallData.Kind.OBJECT -> {
                // TODO(audit): C has no method dispatch. Best-guess: receiver as first arg.
                val receiver = c.receiverPath.joinToString(".")
                val all = if (args.isEmpty()) "&$receiver" else "&$receiver, $args"
                "${receiver}_${c.functionName}($all)"
            }
        }
    }

    override fun emitLambda(l: LambdaFunctionData): String {
        // TODO(audit): C has no anonymous functions. Emitting a placeholder so the surrounding
        // assignment still type-checks against a generic function-pointer-ish identifier.
        return "/* TODO(audit): lambda */ NULL"
    }

    override fun emitArrayLiteral(a: ArrayLiteralData): String {
        // C99 compound literal. Element type is inferred from the first element's kind.
        val elementType = inferArrayElementType(a)
        val body = a.elements.joinToString(", ") { emitExpression(it) }
        return "($elementType[]){$body}"
    }

    /**
     * Infer the C element type for an array literal from its first element's expression
     * kind. Empty arrays default to `int` (TODO: surface a useful diagnostic).
     */
    private fun inferArrayElementType(a: ArrayLiteralData): String {
        if (a.elements.isEmpty()) {
            // TODO(audit): empty array literal — element type can't be inferred from contents.
            return "int"
        }
        val first = a.elements[0]
        return when (first.kind) {
            ExpressionData.Kind.INT_LITERAL -> "int"
            ExpressionData.Kind.DEC_LITERAL -> "double"
            ExpressionData.Kind.BOOL_LITERAL -> { requiredHeaders.add("<stdbool.h>"); "bool" }
            ExpressionData.Kind.STRING_LITERAL -> "const char *"
            // Identifiers, calls, arithmetic, etc. — would need real type inference.
            // TODO(audit): falls back to int; a real fix needs G4.
            else -> "int"
        }
    }

    override fun emitBundleLiteral(b: BundleLiteralData): String {
        // TODO(audit): bundle semantics undefined; emit a placeholder struct literal.
        return "/* TODO(audit): bundle */ {0}"
    }

    private fun joinBody(body: List<TranslatableStatement>): String =
        body.joinToString("\n") { it.translate(this) }
}
