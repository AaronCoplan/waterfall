package com.aaroncoplan.waterfall.compiler.tests;

import com.aaroncoplan.waterfall.compiler.Main;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Smoke tests that exercise the compiler end-to-end on the example files.
 * If something in the AST -> codegen path regresses badly, these break first.
 */
public class EndToEndSmokeTest {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream capturedOut;

    @Before
    public void redirectStdout() {
        originalOut = System.out;
        originalErr = System.err;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
        // Suppress logger noise on stderr to keep the test report clean.
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
    }

    @After
    public void restoreStdout() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void functionWithBodyTranslates() {
        Main.main(new String[]{"../examples/FunctionWithBodyModule.wf"});
        String output = capturedOut.toString();
        assertFalse("Output should be non-empty", output.isEmpty());
        assertTrue("Output should declare flag variable", output.contains("int flag = 0;"));
        assertTrue("Output should declare doSomething function", output.contains("void doSomething()"));
        assertTrue("Output should contain untyped declaration inferred as int", output.contains("int x = 14;"));
    }

    @Test
    public void controlFlowTranslates() {
        Main.main(new String[]{"../examples/ControlFlowModule.wf"});
        String output = capturedOut.toString();
        assertTrue("Output should emit if-block", output.contains("if ("));
        assertTrue("Output should emit else-if-block", output.contains("else if ("));
        assertTrue("Output should emit else-block", output.contains("else {"));
        assertTrue("Output should emit for-block", output.contains("for ("));
        assertTrue("Output should emit local function call",
                output.contains("doSomething()"));
        assertTrue("Output should emit module-qualified function call as Module_fn",
                output.contains("Other_helper(1, 2)"));
    }

    @Test
    public void variableDeclarationsTranslates() {
        Main.main(new String[]{"../examples/VariableDeclarationsModule.wf"});
        String output = capturedOut.toString();
        assertTrue("Output should declare int x = 4;", output.contains("int x = 4;"));
    }

    @Test
    public void emptyModuleProducesNoCode() {
        Main.main(new String[]{"../examples/EmptyModule.wf"});
        String output = capturedOut.toString().trim();
        assertTrue("Empty module should produce empty output, got: [" + output + "]", output.isEmpty());
    }
}
