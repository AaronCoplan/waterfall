package com.aaroncoplan.waterfall.parser

import com.aaroncoplan.waterfall.generated.WaterfallParser

class ParseResult internal constructor(
    private val filePath: String,
    private val syntaxErrors: List<String>,
    private val programAST: WaterfallParser.ProgramContext
) {
    fun getFilePath(): String = filePath
    fun getSyntaxErrors(): List<String> = syntaxErrors
    fun getProgramAST(): WaterfallParser.ProgramContext = programAST
    fun hasErrors(): Boolean = syntaxErrors.isNotEmpty()
}
