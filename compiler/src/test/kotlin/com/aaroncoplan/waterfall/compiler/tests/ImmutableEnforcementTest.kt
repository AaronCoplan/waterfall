package com.aaroncoplan.waterfall.compiler.tests

import com.aaroncoplan.waterfall.compiler.CompilerError
import com.aaroncoplan.waterfall.compiler.Main
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files

/**
 * P0-PR3 negative tests: verifier must reject writes to const/imm bindings.
 *
 * These tests are written from the spec, not from the implementation. They fail
 * until VariableAssignmentData.verify and IncrementStatementData.verify are
 * wired to check immutability in the symbol table.
 *
 * Five cases, one per behaviour the spec requires the verifier to block:
 *   1. const typed binding + plain reassignment (=)
 *   2. const typed binding + compound assignment (+=)
 *   3. const typed binding + increment (++)
 *   4. imm typed binding + plain reassignment (=)
 *   5. const untyped binding (:= form) + plain reassignment (=)
 */
class ImmutableEnforcementTest {

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

    private fun compileSnippet(source: String): CompilerError? {
        val tmp = Files.createTempFile("waterfall-imm-", ".wf")
        return try {
            Files.write(tmp, (source.trimIndent() + "\n").toByteArray())
            var thrown: CompilerError? = null
            try {
                Main.run(arrayOf(tmp.toString()))
            } catch (e: CompilerError) {
                thrown = e
            }
            thrown
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    /**
     * Case 1 — const typed binding + plain assignment.
     * `const int x = 4; x = 5` must be rejected.
     */
    @Test
    fun constTypedBinding_plainAssignment_isRejected() {
        val err = compileSnippet("""
            module ConstPlainAssign {
                func go() {
                    const int x = 4
                    x = 5
                }
            }
        """)
        assertNotNull(
            "Expected CompilerError: assigning to const-typed binding should be rejected",
            err
        )
        assertTrue(
            "stderr should mention immutable binding; got:\n${capturedErr.toString()}",
            capturedErr.toString().contains("immutable binding")
        )
    }

    /**
     * Case 2 — const typed binding + compound assignment.
     * `const int total = 0; total += 3` must be rejected.
     */
    @Test
    fun constTypedBinding_compoundAssignment_isRejected() {
        val err = compileSnippet("""
            module ConstCompound {
                func go() {
                    const int total = 0
                    total += 3
                }
            }
        """)
        assertNotNull(
            "Expected CompilerError: compound-assigning to const-typed binding should be rejected",
            err
        )
        assertTrue(
            "stderr should mention immutable binding; got:\n${capturedErr.toString()}",
            capturedErr.toString().contains("immutable binding")
        )
    }

    /**
     * Case 3 — const typed binding + increment.
     * `const int n = 0; n++` must be rejected.
     */
    @Test
    fun constTypedBinding_increment_isRejected() {
        val err = compileSnippet("""
            module ConstIncrement {
                func go() {
                    const int n = 0
                    n++
                }
            }
        """)
        assertNotNull(
            "Expected CompilerError: incrementing a const-typed binding should be rejected",
            err
        )
        assertTrue(
            "stderr should mention immutable binding; got:\n${capturedErr.toString()}",
            capturedErr.toString().contains("immutable binding")
        )
    }

    /**
     * Case 4 — imm typed binding + plain assignment.
     * `imm int x = 4; x = 99` must be rejected.
     * Guards the imm keyword, which isImmutable() treats identically to const.
     */
    @Test
    fun immTypedBinding_plainAssignment_isRejected() {
        val err = compileSnippet("""
            module ImmPlainAssign {
                func go() {
                    imm int x = 4
                    x = 99
                }
            }
        """)
        assertNotNull(
            "Expected CompilerError: assigning to imm-typed binding should be rejected",
            err
        )
        assertTrue(
            "stderr should mention immutable binding; got:\n${capturedErr.toString()}",
            capturedErr.toString().contains("immutable binding")
        )
    }

    /**
     * Case 5 — const untyped binding (:= form) + plain assignment.
     * `const y := 7; y = 0` must be rejected.
     * UntypedVariableDeclarationAndAssignmentData.isImmutable() also returns true for
     * the const modifier; the verifier must carry the flag into the symbol table.
     */
    @Test
    fun constUntypedBinding_plainAssignment_isRejected() {
        val err = compileSnippet("""
            module ConstUntyped {
                func go() {
                    const y := 7
                    y = 0
                }
            }
        """)
        assertNotNull(
            "Expected CompilerError: assigning to const untyped binding should be rejected",
            err
        )
        assertTrue(
            "stderr should mention immutable binding; got:\n${capturedErr.toString()}",
            capturedErr.toString().contains("immutable binding")
        )
    }
}
