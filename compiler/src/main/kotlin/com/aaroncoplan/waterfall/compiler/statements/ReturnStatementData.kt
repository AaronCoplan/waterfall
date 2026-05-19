package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

class ReturnStatementData(filePath: String, ctx: WaterfallParser.ReturnStatementContext)
    : TranslatableStatement(filePath, ctx) {

    /** Null for a bare `return` with no value. */
    @JvmField val value: ExpressionData? = ctx.expression()?.let { ExpressionData(filePath, it) }

    override fun translate(backend: CodeGenerator): String =
        error("translate() is dead in §5.5 — backends consume IrModule via IrLowering; removed in §5.6")
}
