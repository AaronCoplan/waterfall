package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

class VariableAssignmentData(filePath: String, ctx: WaterfallParser.VariableAssignmentContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text
    /** "=", "+=", "-=", "*=", "/=", or "%=" */
    @JvmField val op: String = ctx.op.text
    @JvmField val value: ExpressionData = ExpressionData(filePath, ctx.expression())

    override fun translate(backend: CodeGenerator): String =
        error("translate() is dead in §5.5 — backends consume IrModule via IrLowering; removed in §5.6")
}
