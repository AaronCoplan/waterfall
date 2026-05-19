package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
class TypedVariableDeclarationAndAssignmentData(
    filePath: String,
    ctx: WaterfallParser.TypedVariableDeclarationAndAssignmentContext
) : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text
    @JvmField val type: String = ctx.type().text
    @JvmField val modifiers: List<String> =
        ctx.modifier()?.map { it.text } ?: emptyList()
    @JvmField val value: ExpressionData = ExpressionData(filePath, ctx.expression())

    /** True iff the binding was declared with `const` or `imm`. */
    fun isImmutable(): Boolean = "const" in modifiers || "imm" in modifiers
}
