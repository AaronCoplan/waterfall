package com.aaroncoplan.waterfall.parser.tests

import org.junit.Test

class FunctionCallTest {

    @Test
    fun testModuleFunctionCallPositionalArgs() {
        val code = "module m {\nfunc f() {\nMyModule::myFunc(a, b, c, 877)\n}\n}"
        TestUtils.shouldPass(code)
    }

    @Test
    fun testModuleFunctionCallNamedArgs() {
        val code = "module m {\nfunc f() {\nMyModule::myFunc(x = 1, b = z)\n}\n}"
        TestUtils.shouldPass(code)
    }

    @Test
    fun testModuleFunctionCallMixedArgs() {
        val code = "module m {\nfunc f() {\nMyModule::myFunc(1, x=2)\n}\n}"
        TestUtils.shouldFail(code)
    }

    @Test
    fun testObjectFunctionCallPositionalArgs() {
        val code = "module m {\nfunc f() {\nmyObject.myFunction(1,2,x, anotherFunc())\n}\n}"
        TestUtils.shouldPass(code)
    }

    @Test
    fun testObjectFunctionCallNamedArgs() {
        val code = "module m {\nfunc f() {\nmyObject.myFunction(x = 1, b = 2, c = [1,2,3])\n}\n}"
        TestUtils.shouldPass(code)
    }

    @Test
    fun testObjectFunctionCallMixedArgs() {
        val code = "module m {\nfunc f() {\nmyObject.myFunction(x = 1, 2, 3)\n}\n}"
        TestUtils.shouldFail(code)
    }
}
