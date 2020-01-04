package com.aaroncoplan.waterfall.parser.tests

import org.junit.Assert

import com.aaroncoplan.waterfall.parser.parseCodeString

open class ParserTest {

    fun assertParsePasses(code: String) {
        val errors = parse(code)
        if(!errors.isEmpty()) {
            print(errors)
        }
        Assert.assertTrue(errors.isEmpty())
    }

    fun assertParseFails(code: String) {
        val errors = parse(code)        
        Assert.assertFalse(errors.isEmpty())
    }

    private fun parse(code: String): List<String> {
        val parseResult = parseCodeString("dummyfilename", code)
        return parseResult.syntaxErrors
    }
}