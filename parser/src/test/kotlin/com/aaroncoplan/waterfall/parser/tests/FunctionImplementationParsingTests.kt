package com.aaroncoplan.waterfall.parser.tests

import org.junit.Test

class FunctionImplementationParsingTests {

    @Test
    fun noArgs() {
        TestUtils.shouldPass("module m {\nfunc myFunc() {}\n}")
    }

    @Test
    fun singleArg() {
        TestUtils.shouldPass("module m {\nfunc myFunc(type1 arg1) {}\n}")
    }

    @Test
    fun multipleArgs() {
        TestUtils.shouldPass(arrayOf(
            "module m {\nfunc myFunc(type1 arg1, type2 arg2) {}\n}",
            "module m {\nfunc myFunc(type1 arg1, type2 arg2, type3 arg3) {}\n}"
        ))
    }

    @Test
    fun withReturnType() {
        TestUtils.shouldPass(arrayOf(
            "module m {\nfunc myFunc(type1 arg1) returns type2 {}\n}",
            "module m {\nfunc myFunc() returns type2 {}\n}",
            "module m {\nfunc myFunc(type1 arg1, type4 arg2) returns type2 {}\n}"
        ))
    }

    @Test
    fun withBody() {
        TestUtils.shouldPass("module m {\nfunc myFunc(type1 arg1) returns type2 {\n x := 5\n}\n}")
    }
}
