package com.aaroncoplan.waterfall.compiler.tests

import com.aaroncoplan.waterfall.compiler.Main
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Smoke tests that exercise the compiler end-to-end on the example files.
 * If something in the AST -> codegen path regresses badly, these break first.
 */
class EndToEndSmokeTest {

    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream
    private lateinit var capturedOut: ByteArrayOutputStream

    @Before
    fun redirectStdout() {
        originalOut = System.out
        originalErr = System.err
        capturedOut = ByteArrayOutputStream()
        System.setOut(PrintStream(capturedOut))
        // Suppress logger noise on stderr to keep the test report clean.
        System.setErr(PrintStream(ByteArrayOutputStream()))
    }

    @After
    fun restoreStdout() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    @Test
    fun functionWithBodyTranslates() {
        Main.run(arrayOf("../examples/FunctionWithBodyModule.wf"))
        val output = capturedOut.toString()
        assertFalse("Output should be non-empty", output.isEmpty())
        assertTrue("Output should declare flag variable", output.contains("int flag = 0;"))
        assertTrue("Output should declare doSomething function", output.contains("void doSomething()"))
        assertTrue("Output should contain untyped declaration inferred as int", output.contains("int x = 14;"))
    }

    @Test
    fun controlFlowTranslates() {
        Main.run(arrayOf("../examples/ControlFlowModule.wf"))
        val output = capturedOut.toString()
        assertTrue("Output should emit if-block", output.contains("if ("))
        assertTrue("Output should emit else-if-block", output.contains("else if ("))
        assertTrue("Output should emit else-block", output.contains("else {"))
        assertTrue("Output should emit for-block", output.contains("for ("))
        assertTrue("Output should emit local function call", output.contains("doSomething()"))
        assertTrue("Output should emit module-qualified function call as Module_fn",
            output.contains("Other_helper(1, 2)"))
    }

    @Test
    fun variableDeclarationsTranslates() {
        Main.run(arrayOf("../examples/VariableDeclarationsModule.wf"))
        val output = capturedOut.toString()
        assertTrue("Output should declare int x = 4;", output.contains("int x = 4;"))
    }

    @Test
    fun emptyModuleProducesNoCode() {
        Main.run(arrayOf("../examples/EmptyModule.wf"))
        val output = capturedOut.toString().trim()
        assertTrue("Empty module should produce empty output, got: [$output]", output.isEmpty())
    }
}
