package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

class FunctionCallStatementData(filePath: String, ctx: WaterfallParser.FunctionCallContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val call: FunctionCallData = FunctionCallData(filePath, ctx)

    override fun translate(backend: CodeGenerator): String =
        error("translate() is dead in §5.5 — backends consume IrModule via IrLowering; removed in §5.6")
}
