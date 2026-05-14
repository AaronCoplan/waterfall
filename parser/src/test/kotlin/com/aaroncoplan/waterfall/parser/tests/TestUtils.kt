package com.aaroncoplan.waterfall.parser.tests

import com.aaroncoplan.waterfall.parser.FileParser
import com.aaroncoplan.waterfall.parser.ParseResult
import org.junit.Assert

object TestUtils {

    private fun execParse(code: String): ParseResult {
        val result = FileParser.parseCodeString(null, code)
        if (result.hasErrors()) {
            result.getSyntaxErrors().forEach { println(it) }
        }
        return result
    }

    @JvmStatic
    fun shouldPass(code: String) {
        Assert.assertFalse(execParse(code).hasErrors())
    }

    @JvmStatic
    fun shouldPass(code: Array<String>) {
        code.forEach { shouldPass(it) }
    }

    @JvmStatic
    fun shouldPass(code: List<String>) {
        code.forEach { shouldPass(it) }
    }

    @JvmStatic
    fun shouldFail(code: String) {
        Assert.assertTrue(execParse(code).hasErrors())
    }

    @JvmStatic
    fun shouldFail(code: Array<String>) {
        code.forEach { shouldFail(it) }
    }

    @JvmStatic
    fun shouldFail(code: List<String>) {
        code.forEach { shouldFail(it) }
    }
}
