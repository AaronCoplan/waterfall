package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.parser.Pair

class FunctionImplementationData(
    filePath: String,
    ctx: WaterfallParser.FunctionImplementationContext
) : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text
    @JvmField val returnType: String? = ctx.returnType?.text
    @JvmField val typedArguments: List<Pair<String, String>> =
        (ctx.typedArgumentList()?.typedArgument() ?: emptyList()).map { arg ->
            Pair(arg.type().text, arg.name.text)
        }
    @JvmField val statements: List<TranslatableStatement> =
        StatementDispatcher.fromStatementBlock(filePath, ctx.statementBlock())
}
