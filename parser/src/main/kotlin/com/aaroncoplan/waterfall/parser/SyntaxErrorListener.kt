package com.aaroncoplan.waterfall.parser

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.misc.ParseCancellationException

internal class SyntaxErrorListener(private val fileName: String) : BaseErrorListener() {

    private val syntaxErrors: MutableList<String> = mutableListOf()

    fun getSyntaxErrors(): List<String> = syntaxErrors

    @Throws(ParseCancellationException::class)
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        val errorMessage = "$fileName line $line:$charPositionInLine $msg"
        syntaxErrors.add(errorMessage)
    }
}
