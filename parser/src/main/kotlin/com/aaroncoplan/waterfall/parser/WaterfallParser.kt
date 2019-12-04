package com.aaroncoplan.waterfall.parser

import java.io.File

import com.aaroncoplan.waterfall.parser.generated.WaterfallParser
import com.aaroncoplan.waterfall.parser.generated.WaterfallLexer

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

data class ParseResult(val filepath: String, val syntaxErrors: List<String>, val ast: WaterfallParser.ProgramContext)

fun parseFile(filepath: String): ParseResult {
    val contents = File(filepath).readText(Charsets.UTF_8)    
    return parseCodeString(filepath, contents)
}

fun parseCodeString(filepath: String, contents: String): ParseResult {
    val syntaxErrorListener = SyntaxErrorListener(filepath)

    val charStream = CharStreams.fromString(contents)

    val lexer = WaterfallLexer(charStream)
    lexer.removeErrorListeners()    

    val tokenStream = CommonTokenStream(lexer)
    
    val parser = WaterfallParser(tokenStream)
    parser.removeErrorListeners()
    parser.addErrorListener(syntaxErrorListener)

    val ast = parser.program();    
    return ParseResult(filepath, syntaxErrorListener.getSyntaxErrors(), ast)
}