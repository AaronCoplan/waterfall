package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
class WhileBlockData(filePath: String, ctx: WaterfallParser.WhileBlockContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val condition: ExpressionData = ExpressionData(filePath, ctx.expression())
    @JvmField val body: List<TranslatableStatement> =
        StatementDispatcher.fromStatementBlock(filePath, ctx.statementBlock())
}
