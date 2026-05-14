package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser

class ArrayLiteralData(filePath: String, ctx: WaterfallParser.ArrayLiteralContext) {
    @JvmField val elements: List<ExpressionData> =
        ctx.positionalArgumentList().expression().map { ExpressionData(filePath, it) }
}
