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

    @Test
    fun testFunctionCallWithNoArgumentsPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                print();    
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithSingleVariableArgumentPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                print(variableName);
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithMultipleVariableArgumentsPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                print(var1, var2, var3);    
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithSingleLiteralArgumentPasses() {
        assertParsePasses(
            wrapInFunction(
            """
                print(`I am a string`);
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithMultipleLiteralArgumentsPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                print(1, 2, 3); 
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithMultipleVariableAndLiteralArgumentsPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                print(1, myVar1, 2, myVar2, `hello`); 
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithSingleVariableNamedArgumentPasses() {
        assertParsePasses(
            wrapInFunction(
            """
                print(myArg=variableName);
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithMultipleVariableNamedArgumentsPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                print(arg1=var1, arg2=var2, arg3=var3); 
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithSingleLiteralNamedArgumentPasses() {
        assertParsePasses(
            wrapInFunction(
            """
                print(myString=`I am a string`);
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithMultipleLiteralNamedArgumentsPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                print(int1=1, int2=2, int3=3); 
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithMultipleVariableAndLiteralNamedArgumentsPasses() {
        assertParsePasses(
            wrapInFunction(
                """
                print(int1=1, arg1=var1, string1=`hello`, var2=var2); 
                """
            )
        )
    }

    @Test
    fun testFunctionCallWithMixedArgumentsAndNamedArgumentsFails() {
        assertParseFails(
            wrapInFunction(
                """
                print(123, `mystring`, arg2=2); 
                """
            )
        )
    }
}