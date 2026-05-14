package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.parser.Pair

class LambdaFunctionData(filePath: String, ctx: WaterfallParser.LambdaFunctionContext) {

    @JvmField
    val typedArguments: List<Pair<String, String>> =
        (ctx.typedArgumentList()?.typedArgument() ?: emptyList()).map { a ->
            Pair(a.type().text, a.name.text)
        }

    /** Null when the source has an empty body `{}`. */
    @JvmField
    val body: FunctionCallData? =
        ctx.functionCall()?.let { FunctionCallData(filePath, it) }
}
