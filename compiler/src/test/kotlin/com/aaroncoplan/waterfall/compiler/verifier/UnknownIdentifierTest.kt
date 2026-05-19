package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.parser.FileParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Functional tests for [VerifyError.UnknownIdentifier] — closes OQ-3=C (P10 carry-forward).
 *
 * **7 mandatory cases** per the post-skeptic-edit spec count (§4.1, verified 2026-05-19).
 * Plus 2 additional cases blessed by Aaron for completeness:
 *   - [decrementUnknownIdentifierFails]: same emission site as increment; cheap one-liner.
 *   - [moduleCallDoesNotEmitUnknownIdentifier]: PITFALL #17 regression guard.
 *
 * **Emission sites covered by §4.1** (4 total per OQ-11.6=strict):
 *   1. [StatementVerifier.verifyVarAssignment] → Context.ASSIGNMENT_LHS
 *   2. [StatementVerifier.verifyIncrement] → Context.INCREMENT_TARGET
 *   3. [StatementVerifier.verifyForBlock] → Context.FOR_COLLECTION
 *   4. [StatementVerifier.verifyFunctionCallStatement] (LOCAL kind) → Context.EXPRESSION
 *
 * Expression-context IDENTIFIER emission (RHS of var decl, if/while conditions, etc.)
 * is handled by [Elaboration] per OQ-11.3=(a) and tested via
 * [UnknownIdentifierPropertyTest] Property 2 + [ExpressionVerifierWalkerSkeletonTest].
 *
 * **Style:** pure JUnit 4 (`@Test`, `Assert.*`). No Kotest dependency.
 */
class UnknownIdentifierTest {

    private fun parseAndAst(source: String): ModuleAst {
        val parseResult = FileParser.parseCodeString("test.wf", source)
        return ModuleAst("test.wf", parseResult.getProgramAST().module())
    }

    // ------------------------------------------------------------------ //
    // Mandatory case 1 — ASSIGNMENT_LHS
    // ------------------------------------------------------------------ //

    /**
     * Assigning to an undeclared LHS emits UnknownIdentifier(ASSIGNMENT_LHS).
     * Closes the P10 no-op (see VerifierTest.assignToUndeclaredLhsNowEmitsUnknownIdentifier).
     * Error code WF1201; message contains "Cannot assign to undeclared identifier".
     */
    @Test fun assignmentLhsUnknownIdentifierFails() {
        val module = parseAndAst("""
            module Foo {
                func go() {
                    undeclaredVar = 5
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertFalse("Expected verify failure for undeclared LHS assignment", result.isSuccessful)
        val errors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertEquals("Expected exactly 1 UnknownIdentifier error", 1, errors.size)
        val err = errors[0]
        assertEquals("undeclaredVar", err.name)
        assertEquals(VerifyError.UnknownIdentifier.Context.ASSIGNMENT_LHS, err.context)
        assertEquals("WF1201", err.code)
        assertTrue(
            "Message should contain 'Cannot assign to undeclared identifier'",
            err.message.contains("Cannot assign to undeclared identifier")
        )
    }

    // ------------------------------------------------------------------ //
    // Mandatory case 2 — INCREMENT_TARGET (++)
    // ------------------------------------------------------------------ //

    /**
     * Incrementing an undeclared target emits UnknownIdentifier(INCREMENT_TARGET).
     */
    @Test fun incrementUnknownIdentifierFails() {
        val module = parseAndAst("""
            module Foo {
                func go() {
                    undeclaredVar++
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertFalse("Expected verify failure for undeclared increment target", result.isSuccessful)
        val errors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertEquals(1, errors.size)
        val err = errors[0]
        assertEquals("undeclaredVar", err.name)
        assertEquals(VerifyError.UnknownIdentifier.Context.INCREMENT_TARGET, err.context)
        assertTrue(err.message.contains("Cannot increment undeclared identifier"))
    }

    // ------------------------------------------------------------------ //
    // Additional case — INCREMENT_TARGET (--)
    // ------------------------------------------------------------------ //

    /**
     * Decrementing an undeclared target also emits UnknownIdentifier(INCREMENT_TARGET).
     * Same emission path as [incrementUnknownIdentifierFails]; tests that the
     * verifyIncrement path fires for both ++ and -- operators.
     */
    @Test fun decrementUnknownIdentifierFails() {
        val module = parseAndAst("""
            module Foo {
                func go() {
                    undeclaredVar--
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertFalse("Expected verify failure for undeclared decrement target", result.isSuccessful)
        val errors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertEquals(1, errors.size)
        assertEquals(VerifyError.UnknownIdentifier.Context.INCREMENT_TARGET, errors[0].context)
        assertEquals("undeclaredVar", errors[0].name)
    }

    // ------------------------------------------------------------------ //
    // Mandatory case 3 — FOR_COLLECTION (OQ-11.6=strict)
    // ------------------------------------------------------------------ //

    /**
     * A for-loop with an undeclared collection emits UnknownIdentifier(FOR_COLLECTION).
     * OQ-11.6 = strict (Aaron, 2026-05-19): `for(item in undeclared)` is rejected.
     * This is the path that makes ControlFlowModule golden go empty.
     */
    @Test fun forCollectionUnknownIdentifierFails() {
        val module = parseAndAst("""
            module Foo {
                func go() {
                    for(item in undeclaredCollection) {
                        int x = 0
                    }
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertFalse("Expected verify failure for undeclared for-collection", result.isSuccessful)
        val errors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertTrue("Expected at least 1 UnknownIdentifier error", errors.isNotEmpty())
        val collectionError = errors.find { it.context == VerifyError.UnknownIdentifier.Context.FOR_COLLECTION }
        assertFalse("Expected FOR_COLLECTION error for 'undeclaredCollection'", collectionError == null)
        assertEquals("undeclaredCollection", collectionError!!.name)
        assertTrue(collectionError.message.contains("Unknown collection identifier"))
    }

    // ------------------------------------------------------------------ //
    // Mandatory case 7 — OQ-11.6=strict emission site 4: undeclared LOCAL call
    // ------------------------------------------------------------------ //

    /**
     * Calling an undeclared LOCAL function in statement position emits
     * UnknownIdentifier(EXPRESSION) via [StatementVerifier.verifyFunctionCallStatement].
     * This is the path that makes WhileModule golden go empty.
     * OQ-11.6 = strict: `doSomething()` where doSomething is undeclared is rejected.
     */
    @Test fun undeclaredLocalFunctionCallStatementFails() {
        val module = parseAndAst("""
            module Foo {
                func go() {
                    doSomething()
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertFalse("Expected verify failure for undeclared LOCAL function call", result.isSuccessful)
        val errors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertEquals("Expected exactly 1 UnknownIdentifier error", 1, errors.size)
        val err = errors[0]
        assertEquals("doSomething", err.name)
        assertEquals(VerifyError.UnknownIdentifier.Context.EXPRESSION, err.context)
    }

    // ------------------------------------------------------------------ //
    // Additional case — PITFALL #17 regression: MODULE calls are NOT checked
    // ------------------------------------------------------------------ //

    /**
     * MODULE-kind function calls (e.g., Other::method()) must NOT emit
     * UnknownIdentifier. Only LOCAL kind calls are checked per OQ-11.6=strict.
     *
     * PITFALL #17 from PHASE-11-design.md §5: an implementer who misreads the
     * verifyFunctionCallStatement change as "check ALL calls regardless of kind"
     * would break module calls. This test guards that regression.
     */
    @Test fun moduleCallDoesNotEmitUnknownIdentifier() {
        val module = parseAndAst("""
            module Foo {
                func go() {
                    Other::doSomething(1, 2)
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        val unknownIdErrors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertTrue(
            "MODULE-kind calls must NOT emit UnknownIdentifier; got: $unknownIdErrors",
            unknownIdErrors.isEmpty()
        )
    }

    // ------------------------------------------------------------------ //
    // Mandatory case 4 — Positive control: declared identifier
    // ------------------------------------------------------------------ //

    /**
     * A declared identifier used as LHS of assignment must NOT emit UnknownIdentifier.
     * Guards against over-rejection: `int x = 5; x = 6` is valid Waterfall.
     */
    @Test fun declaredIdentifierStillResolves() {
        val module = parseAndAst("""
            module Foo {
                func go() {
                    int x = 5
                    x = 6
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        val unknownIdErrors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertTrue(
            "Declared identifier should not emit UnknownIdentifier; got: $unknownIdErrors",
            unknownIdErrors.isEmpty()
        )
    }

    // ------------------------------------------------------------------ //
    // Mandatory case 5 — Positive control: module-scope visibility
    // ------------------------------------------------------------------ //

    /**
     * A module-scope (top-level) variable is visible inside function bodies.
     * Guards against a regression where module-level declarations are not
     * propagated into function scope before body verification.
     */
    @Test fun topLevelVarReferencedInFunctionStillResolves() {
        val module = parseAndAst("""
            module Foo {
                int limit = 100
                func enforce() {
                    limit = 50
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        val unknownIdErrors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertTrue(
            "Top-level var should be visible in function body; got UnknownIdentifier: $unknownIdErrors",
            unknownIdErrors.isEmpty()
        )
    }

    // ------------------------------------------------------------------ //
    // Mandatory case 6 — Positive control: forward function reference (Pass 1.5)
    // ------------------------------------------------------------------ //

    /**
     * A function declared after its caller must still resolve without UnknownIdentifier.
     * Guards the §5.4 Pass 1.5 forward-reference fix: ModuleVerifier self-declares all
     * functions before elaborating any function body, so `a()` calling `b()` where
     * `b` is declared later in the same module is valid.
     */
    @Test fun forwardFunctionReferenceStillResolves() {
        val module = parseAndAst("""
            module Foo {
                func caller() {
                    callee()
                }
                func callee() {
                    int x = 0
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        val unknownIdErrors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertTrue(
            "Forward-declared function callee() should resolve without UnknownIdentifier; got: $unknownIdErrors",
            unknownIdErrors.isEmpty()
        )
    }
}
