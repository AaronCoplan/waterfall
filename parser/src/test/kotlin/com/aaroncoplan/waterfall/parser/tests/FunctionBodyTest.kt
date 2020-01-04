package com.aaroncoplan.waterfall.parser.tests

import org.junit.Test

class FunctionBodyTest : ParserTest() {
    private fun wrapInFunction(code: String): String {        
        return """
        module name {
            func name(type name) returns type {
                ${code}
            }
        }
        """
    }

    @Test
    fun testEmptyFunctionBodyPasses() {
        assertParsePasses(
            wrapInFunction("")                
        )   
    }

    @Test
    fun testVariableAssignmentIntLiteralPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                x := 4;
                """
            )
        )
    }

    @Test
    fun testVariableAssignmentDecLiteralPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                y := 3.2546678765908098;
                """
            )
        )
    }

    @Test
    fun testVariableAssignmentStringLiteralPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                message := `hello`;
                """
            )
        )
    }

    @Test
    fun testVariableReassignmentIntLiteralPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                x = 4;
                """
            )
        )
    }

    @Test
    fun testVariableReassignmentDecLiteralPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                y = 1.142543565;
                """
            )
        )
    }

    @Test
    fun testVariableReassignmentStringLiteralPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                msg = `hello`;
                """
            )
        )
    }

    @Test
    fun testTypedVariableAssignmentIntLiteralPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                int x = 4;
                """       
            )
        )
    }

    @Test
    fun testTypedVariableAssignmentDecLiteralPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                dec x = 123.1231242983579287;
                """       
            )
        )
    }

    @Test
    fun testTypedVariableAssignmentStringLiteralPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                string msg = `hello`;
                """       
            )
        )
    }
}