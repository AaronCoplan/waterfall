package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
class UntypedVariableDeclarationAndAssignmentData(
    filePath: String,
    ctx: WaterfallParser.UntypedVariableDeclarationAndAssignmentContext
) : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text
    @JvmField val modifiers: List<String> =
        ctx.modifier()?.map { it.text } ?: emptyList()
    @JvmField val value: ExpressionData = ExpressionData(filePath, ctx.expression())
    @JvmField val inferredType: String = inferType(value)

    /** True iff the binding was declared with `const` or `imm`. */
    fun isImmutable(): Boolean = "const" in modifiers || "imm" in modifiers

    companion object {
        private fun inferType(expr: ExpressionData): String = when (expr.kind) {
            ExpressionData.Kind.INT_LITERAL -> "int"
            ExpressionData.Kind.DEC_LITERAL -> "dec"
            // TODO(audit): no first-class string type yet; defaults to char so the
            // C backend can emit `char *`. JS/Python don't care.
            ExpressionData.Kind.STRING_LITERAL -> "char"
            // TODO(audit): cross-expression type inference (identifiers, calls,
            // arithmetic) not implemented; default int.
            else -> "int"
        }
    }
}
