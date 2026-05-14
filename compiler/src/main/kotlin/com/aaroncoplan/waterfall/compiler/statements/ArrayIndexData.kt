package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser

class ArrayIndexData(filePath: String, ctx: WaterfallParser.ArrayIndexContext) {
    @JvmField val target: String = ctx.target.text
    @JvmField val index: ExpressionData = ExpressionData(filePath, ctx.index)
}
