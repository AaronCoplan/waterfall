package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.parser.FileParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sanity tests for the P10 [JoinAnalysis] stub.
 *
 * The stub walks branch bodies for declaration/immutability errors but does NOT run
 * the readonly intersection (deferred to P12 when the `readonly x` grammar rule lands).
 *
 * These tests verify:
 *   1. Errors inside branch bodies ARE collected (guards the SA-1 trap: a pure
 *      `return emptyList()` stub would silently swallow errors inside branches).
 *   2. The stub does not accidentally propagate readonly state from a branch body
 *      to the enclosing scope (regression guard for the P12 intersection addition).
 *
 * Tests go through [Verifier.verifyModule] end-to-end; [JoinAnalysis.verifyIfBlock]
 * and [JoinAnalysis.verifyWhileBlock] are reached via [StatementVerifier]'s dispatch.
 */
class JoinAnalysisStubTest {

    // ------------------------------------------------------------------ //
    // Test helper
    // ------------------------------------------------------------------ //

    private fun parseAndAst(source: String): ModuleAst {
        val parseResult = FileParser.parseCodeString("stub-test.wf", source)
        return ModuleAst("stub-test.wf", parseResult.getProgramAST().module())
    }

    // ------------------------------------------------------------------ //
    // Spec-locked case (§4.7 JoinAnalysis stub sanity test)
    // ------------------------------------------------------------------ //

    @Test fun ifElseBodyErrorsAreCollected() {
        // Confirms the stub walks branch bodies and collects errors.
        // Would silently pass if the stub returned emptyList() — the SA-1 trap.
        val module = parseAndAst("""
            module Foo {
                func go(int flag) {
                    if(flag) {
                        int x = 1
                        int x = 2
                    } else {
                        int y = 1
                        int y = 2
                    }
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(2, result.errors.size)
        assertTrue(result.errors.all { it is VerifyError.DuplicateDeclaration })
    }

    // ------------------------------------------------------------------ //
    // Additional body-walking and scope-isolation checks
    // ------------------------------------------------------------------ //

    @Test fun whileBlockBodyErrorsAreCollected() {
        // Analogous to ifElseBodyErrorsAreCollected but for while bodies.
        // Guards the SA-1 trap in [JoinAnalysis.verifyWhileBlock].
        val module = parseAndAst("""
            module Foo {
                func go() {
                    while(true) {
                        int x = 1
                        int x = 2
                    }
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.DuplicateDeclaration)
    }

    @Test fun joinAnalysisStubDoesNotPropagateReadonlyToOuterScope() {
        // A mutable variable declared before an if block must remain mutable
        // after the if block. Confirms that verifyBranch uses a fresh child scope
        // (not the parent scope directly) and discards the snapshot without
        // committing any readonly state to the parent.
        //
        // This is a regression guard for the P12 intersection implementation:
        // even after P12 adds commitReadonly logic, this specific case (x was never
        // promoted inside any branch) must continue to succeed.
        val module = parseAndAst("""
            module Foo {
                func go(int flag) {
                    int x = 1
                    if(flag) {
                        const int y = 1
                    }
                    x = 2
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertTrue(result.isSuccessful)
    }
}
