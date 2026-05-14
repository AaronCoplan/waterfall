package com.aaroncoplan.waterfall.parser.tests

import org.junit.Test

class ForBlockTests {

    private val template = "module m {\nfunc f() {\nmyList := [1,2,3]\n%s\n}\n}"

    @Test
    fun testForInEmptyBlock() {
        TestUtils.shouldPass(template.format("for (x in myList) {}"))
    }

    @Test
    fun testForInWithStatements() {
        TestUtils.shouldPass(template.format("for (x in myList){\nprint(x)\n}"))
    }
}
