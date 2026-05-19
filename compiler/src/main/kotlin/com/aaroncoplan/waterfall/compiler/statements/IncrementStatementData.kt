package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
class IncrementStatementData(filePath: String, ctx: WaterfallParser.IncrementStatementContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text
    /** "++" or "--" */
    @JvmField val op: String = ctx.op.text
}
