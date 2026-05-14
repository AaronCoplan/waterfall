package com.aaroncoplan.waterfall.parser.tests

import org.junit.Test

class ModuleParsingTests {

    @Test
    fun noWhiteSpace() {
        TestUtils.shouldPass("module a{}")
    }

    @Test
    fun bracketOnNewline() {
        TestUtils.shouldPass("module a\n{}")
    }

    @Test
    fun bracketTwoNewlinesAway() {
        TestUtils.shouldFail("module a\n\n{}")
    }

    @Test
    fun newlinesBetweenBrackets() {
        TestUtils.shouldPass(arrayOf(
            "module a\n{\n\n}",
            "module b{\n\n\n\n}"
        ))
    }

    @Test
    fun testIllegalModuleNames() {
        TestUtils.shouldFail(arrayOf(
            "module 9startswithnumber {}",
            "module \$dollar\$ign {}",
            "module A_B_&illegalcharampersand {}"
        ))
    }

    @Test
    fun testSingleLineInModule() {
        TestUtils.shouldPass("module a\n{\n int variable2 = 5\n }")
    }
}
