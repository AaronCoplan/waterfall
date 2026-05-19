package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
class ReturnStatementData(filePath: String, ctx: WaterfallParser.ReturnStatementContext)
    : TranslatableStatement(filePath, ctx) {

    /** Null for a bare `return` with no value. */
    @JvmField val value: ExpressionData? = ctx.expression()?.let { ExpressionData(filePath, it) }
}
