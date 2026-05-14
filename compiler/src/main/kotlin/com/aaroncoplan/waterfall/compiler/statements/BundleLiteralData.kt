package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser

class BundleLiteralData(filePath: String, ctx: WaterfallParser.BundleLiteralContext) {
    @JvmField val elements: List<ExpressionData> =
        ctx.positionalArgumentList().expression().map { ExpressionData(filePath, it) }
}
