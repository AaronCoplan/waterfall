package com.aaroncoplan.waterfall.compiler.tests;

import com.aaroncoplan.waterfall.compiler.CompilerError;
import com.aaroncoplan.waterfall.compiler.Main;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Phase 8e's smoke test: a function body that declares the same name twice
 * should be rejected by the verifier. Pre-8e this passed silently because
 * inner var-decls didn't declare into the scope.
 */
public class DuplicateInnerDeclarationTest {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream capturedErr;

    @Before
    public void redirectStreams() {
        originalOut = System.out;
        originalErr = System.err;
        capturedErr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        System.setErr(new PrintStream(capturedErr));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void innerDuplicateIsRejected() throws IOException {
        Path tmp = Files.createTempFile("waterfall-dup-", ".wf");
        try {
            Files.write(tmp,
                    ("module DupInside {\n"
                   + "    func go() {\n"
                   + "        int x = 1\n"
                   + "        int x = 2\n"
                   + "    }\n"
                   + "}\n").getBytes());
            CompilerError thrown = null;
            try {
                Main.run(new String[]{tmp.toString()});
            } catch (CompilerError e) {
                thrown = e;
            }
            assertNotNull("Expected CompilerError on duplicate inner declaration", thrown);
            String stderr = capturedErr.toString();
            assertTrue("stderr should mention duplicate declaration; got:\n" + stderr,
                    stderr.contains("Duplicate declaration"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void shadowingOuterParameterIsRejected() throws IOException {
        // Function parameter `n`, then a local `int n = ...` shadowing it.
        // SymbolTable's declare() walks ancestors and rejects shadowing,
        // so this should be caught.
        Path tmp = Files.createTempFile("waterfall-dup-", ".wf");
        try {
            Files.write(tmp,
                    ("module ShadowArg {\n"
                   + "    func go(int n) {\n"
                   + "        int n = 1\n"
                   + "    }\n"
                   + "}\n").getBytes());
            CompilerError thrown = null;
            try {
                Main.run(new String[]{tmp.toString()});
            } catch (CompilerError e) {
                thrown = e;
            }
            assertNotNull("Expected CompilerError when an inner decl shadows a function param",
                    thrown);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void differentBranchesDontConflict() throws IOException {
        // `int x` in the if-branch and `int x` in the else-branch are independent.
        Path tmp = Files.createTempFile("waterfall-dup-", ".wf");
        try {
            Files.write(tmp,
                    ("module DistinctBranches {\n"
                   + "    func go(int flag) {\n"
                   + "        if(flag) {\n"
                   + "            int x = 1\n"
                   + "        } else {\n"
                   + "            int x = 2\n"
                   + "        }\n"
                   + "    }\n"
                   + "}\n").getBytes());
            try {
                Main.run(new String[]{tmp.toString()});
            } catch (CompilerError e) {
                fail("Decls in mutually exclusive if/else branches should not conflict; got: "
                        + e.getMessage() + "\nstderr: " + capturedErr.toString());
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
