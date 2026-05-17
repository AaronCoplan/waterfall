package com.aaroncoplan.waterfall.compiler.statements.helpers

data class SourcePosition(
    val fileName: String,
    val line: Int,
    val column: Int
) {
    fun generateMessage(): String = "$fileName at $line:$column"
}
