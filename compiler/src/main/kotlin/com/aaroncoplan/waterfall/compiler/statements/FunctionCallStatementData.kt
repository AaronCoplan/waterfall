package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
class FunctionCallStatementData(filePath: String, ctx: WaterfallParser.FunctionCallContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val call: FunctionCallData = FunctionCallData(filePath, ctx)
}
