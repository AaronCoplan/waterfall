package com.aaroncoplan.waterfall.parser

import com.aaroncoplan.waterfall.generated.WaterfallLexer
import com.aaroncoplan.waterfall.generated.WaterfallParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

object FileParser {

    @JvmStatic
    fun parseFile(filePath: String): ParseResult {
        val fileContents = FileUtils.readFile(filePath) ?: ""
        return parseCodeString(filePath, fileContents)
    }

    /**
     * `filePath` may be null for parsing in-memory code (used by tests). Diagnostics
     * just substitute an empty path in that case.
     */
    @JvmStatic
    fun parseCodeString(filePath: String?, codeString: String): ParseResult {
        val charStream = CharStreams.fromString(codeString)
        val lexer = WaterfallLexer(charStream)
        lexer.removeErrorListeners()
        val tokenStream = CommonTokenStream(lexer)

        val parser = WaterfallParser(tokenStream)
        parser.removeErrorListeners()
        val errorListener = SyntaxErrorListener(filePath ?: "")
        parser.addErrorListener(errorListener)

        val programAST = parser.program()
        return ParseResult(filePath ?: "", errorListener.getSyntaxErrors(), programAST)
    }
}
