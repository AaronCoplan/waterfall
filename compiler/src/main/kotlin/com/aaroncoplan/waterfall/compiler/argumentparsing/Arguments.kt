package com.aaroncoplan.waterfall.compiler.argumentparsing

class Arguments(private val files: List<String>, private val target: String) {
    fun getFiles(): List<String> = files
    fun getTarget(): String = target
}
