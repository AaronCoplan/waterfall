package com.aaroncoplan.waterfall.compiler.statements.helpers

import org.antlr.v4.runtime.ParserRuleContext

abstract class TranslatableStatement(
    filePath: String,
    parserRuleContext: ParserRuleContext
) : Translatable {

    private val sourcePosition: SourcePosition = SourcePosition(
        filePath,
        parserRuleContext.start.line,
        parserRuleContext.start.charPositionInLine
    )

    fun getSourcePosition(): SourcePosition = sourcePosition
}
