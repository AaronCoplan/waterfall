package com.aaroncoplan.waterfall.compiler.statements.helpers

class SourcePosition internal constructor(
    private val fileName: String,
    private val line: Int,
    private val column: Int
) {
    fun generateMessage(): String = "$fileName at $line:$column"
}
