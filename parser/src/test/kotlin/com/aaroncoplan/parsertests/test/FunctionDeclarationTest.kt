package com.aaroncoplan.parsertests.test

import org.junit.Test

class FunctionDeclarationTest : ParserTest() {
    private fun wrapInModule(code: String): String {        
        return """
        module name {
            ${code}
        }
        """
    }    

    @Test
    fun testFunctionDeclarationWithNoParamsAndNoReturnTypePasses() {
        assertParsePasses(
            wrapInModule(
                """
                func x() {}
                """
            )
        )   
    }

    @Test
    fun testFunctionDeclarationWithReturnTypeAndNoParamsPasses() {
        assertParsePasses(
            wrapInModule(
                """
                func x() returns bool {}
                """
            )
        )
    }

    @Test
    fun testFunctionDeclarationWithArayReturnTypeAndNoParamsPasses() {
        assertParsePasses(
            wrapInModule(
                """
                func x() returns bool[] {}
                """
            )
        )
    }

    @Test
    fun testFunctionDeclarationWithReturnTypeAndSingleParamPasses() {
        assertParsePasses(
            wrapInModule(
                """
                func x(int a) returns bool {}
                """
            )
        )
    }

    @Test
    fun testFunctionDeclarationWithReturnTypeAndSingleArrayParamPasses() {
        assertParsePasses(
            wrapInModule(
                """
                func x(int[] a) returns bool {}
                """
            )
        )
    }

    @Test
    fun testFunctionDeclarationWithReturnTypeAndMultipleParamsPasses() {
        assertParsePasses(
            wrapInModule(
                """
                func x(int a, int b) returns bool {}
                """
            )
        )
    }

    @Test
    fun testFunctionDeclarationWithReturnTypeAndMultipleArrayParamsPasses() {
        assertParsePasses(
            wrapInModule(
                """
                func x(int[] a, bool[] b) returns bool {}
                """
            )
        )
    }
}