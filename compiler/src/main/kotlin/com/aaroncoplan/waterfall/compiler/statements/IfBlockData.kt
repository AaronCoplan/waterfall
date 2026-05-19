package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
class IfBlockData(filePath: String, ctx: WaterfallParser.IfBlockContext)
    : TranslatableStatement(filePath, ctx) {

    class Branch(
        @JvmField val condition: ExpressionData,
        @JvmField val body: List<TranslatableStatement>
    )

    @JvmField val ifBranch: Branch = Branch(
        ExpressionData(filePath, ctx.expression()),
        StatementDispatcher.fromStatementBlock(filePath, ctx.statementBlock())
    )

    @JvmField val elifBranches: List<Branch> = (ctx.elifBlock() ?: emptyList()).map { eb ->
        Branch(
            ExpressionData(filePath, eb.expression()),
            StatementDispatcher.fromStatementBlock(filePath, eb.statementBlock())
        )
    }

    /** Null when there's no `else` clause. */
    @JvmField val elseBody: List<TranslatableStatement>? = ctx.elseBlock()?.let {
        StatementDispatcher.fromStatementBlock(filePath, it.statementBlock())
    }
}
