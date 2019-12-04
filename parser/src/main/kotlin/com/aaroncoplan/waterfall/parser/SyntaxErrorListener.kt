package com.aaroncoplan.waterfall.parser

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.atn.ATNSimulator
import org.antlr.v4.runtime.misc.ParseCancellationException

class SyntaxErrorListener : BaseErrorListener {

    private val filepath: String
    private val syntaxErrors: ArrayList<String>

    constructor(filepath: String) {
        this.filepath = filepath
        this.syntaxErrors = ArrayList()        
    }

    fun getSyntaxErrors(): List<String> {
        return syntaxErrors
    }

    @Throws(ParseCancellationException::class)
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        val errorMessage = filepath + " line " + line + ":" + charPositionInLine + " " + msg                
        syntaxErrors.add(errorMessage)
    }
}