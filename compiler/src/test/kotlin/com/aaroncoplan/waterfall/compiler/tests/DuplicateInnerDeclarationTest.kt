package com.aaroncoplan.waterfall.compiler.tests

import com.aaroncoplan.waterfall.compiler.CompilerError
import com.aaroncoplan.waterfall.compiler.Main
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files

/**
 * Phase 8e's smoke test: a function body that declares the same name twice
 * should be rejected by the verifier. Pre-8e this passed silently because
 * inner var-decls didn't declare into the scope.
 */
class DuplicateInnerDeclarationTest {

    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream
    private lateinit var capturedErr: ByteArrayOutputStream

    @Before
    fun redirectStreams() {
        originalOut = System.out
        originalErr = System.err
        capturedErr = ByteArrayOutputStream()
        System.setOut(PrintStream(ByteArrayOutputStream()))
        System.setErr(PrintStream(capturedErr))
    }

    @After
    fun restoreStreams() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    @Test
    fun innerDuplicateIsRejected() {
        val tmp = Files.createTempFile("waterfall-dup-", ".wf")
        try {
            Files.write(tmp, ("""
                module DupInside {
                    func go() {
                        int x = 1
                        int x = 2
                    }
                }
            """.trimIndent() + "\n").toByteArray())
            var thrown: CompilerError? = null
            try {
                Main.run(arrayOf(tmp.toString()))
            } catch (e: CompilerError) {
                thrown = e
            }
            assertNotNull("Expected CompilerError on duplicate inner declaration", thrown)
            val stderr = capturedErr.toString()
            assertTrue("stderr should mention duplicate declaration; got:\n$stderr",
                stderr.contains("Duplicate declaration"))
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    @Test
    fun shadowingOuterParameterIsRejected() {
        // Function parameter `n`, then a local `int n = ...` shadowing it.
        // SymbolTable's declare() walks ancestors and rejects shadowing,
        // so this should be caught.
        val tmp = Files.createTempFile("waterfall-dup-", ".wf")
        try {
            Files.write(tmp, ("""
                module ShadowArg {
                    func go(int n) {
                        int n = 1
                    }
                }
            """.trimIndent() + "\n").toByteArray())
            var thrown: CompilerError? = null
            try {
                Main.run(arrayOf(tmp.toString()))
            } catch (e: CompilerError) {
                thrown = e
            }
            assertNotNull("Expected CompilerError when an inner decl shadows a function param", thrown)
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    @Test
    fun differentBranchesDontConflict() {
        // `int x` in the if-branch and `int x` in the else-branch are independent.
        val tmp = Files.createTempFile("waterfall-dup-", ".wf")
        try {
            Files.write(tmp, ("""
                module DistinctBranches {
                    func go(int flag) {
                        if(flag) {
                            int x = 1
                        } else {
                            int x = 2
                        }
                    }
                }
            """.trimIndent() + "\n").toByteArray())
            try {
                Main.run(arrayOf(tmp.toString()))
            } catch (e: CompilerError) {
                fail("Decls in mutually exclusive if/else branches should not conflict; got: " +
                    "${e.message}\nstderr: $capturedErr")
            }
        } finally {
            Files.deleteIfExists(tmp)
        }
    }
}
