package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.parser.FileParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §4.1 skeleton test for [ExpressionVerifier].
 *
 * **ExpressionVerifier returns emptyList() for ALL [com.aaroncoplan.waterfall.compiler.statements.ExpressionData.Kind]
 * values in §4.1.** Per OQ-11.3=(a), expression-context IDENTIFIER emission lives in
 * [Elaboration], not in the walker. The walker's `when` dispatch is the §4.1 deliverable;
 * the full bodies (CAST, LAMBDA, BINARY_OP, FUNCTION_CALL argument verification, ARRAY_INDEX)
 * fill in §4.2.
 *
 * **Two cases:**
 * 1. [walkerDispatchesOnKindWithoutCrashing] — a program exercising multiple expression kinds
 *    with all identifiers declared verifies successfully; ExpressionVerifier produces no errors.
 * 2. [walkerReturnsEmptyListForIdentifierInSkeleton] — an undeclared IDENTIFIER in expression
 *    context produces UnknownIdentifier via Elaboration (OQ-11.3=(a)), NOT via ExpressionVerifier.
 *    ExpressionVerifier's own contribution for IDENTIFIER is emptyList() in §4.1.
 *
 * **Style:** pure JUnit 4 (`@Test`, `Assert.*`). No Kotest dependency.
 *
 * **§4.2 update note:** when §4.2 lands, this file should be extended (or replaced by
 * ExpressionVerifierTest.kt) with full-body tests for CAST, LAMBDA, BINARY_OP, FUNCTION_CALL,
 * ARRAY_INDEX kind dispatch.
 */
class ExpressionVerifierWalkerSkeletonTest {

    private fun parseAndAst(source: String): ModuleAst {
        val parseResult = FileParser.parseCodeString("test.wf", source)
        return ModuleAst("test.wf", parseResult.getProgramAST().module())
    }

    /**
     * A program exercising INT_LITERAL, DEC_LITERAL, BOOL_LITERAL, ARRAY,
     * BINARY_OP, FUNCTION_CALL (LOCAL declared), and IDENTIFIER (declared) expression
     * kinds verifies successfully. ExpressionVerifier returns emptyList() for all of
     * these in §4.1 skeleton state; no spurious errors are produced by the dispatch.
     */
    @Test fun walkerDispatchesOnKindWithoutCrashing() {
        val module = parseAndAst("""
            module Skeleton {
                func add(int a, int b) returns int {
                    return a + b
                }
                func exercise() {
                    int x = 5
                    dec d = 1.5
                    bool flag = true
                    int[] arr = [1, 2, 3]
                    int result = add(x, x)
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        // No UnknownIdentifier errors; all identifiers declared; skeleton dispatch is clean
        val unknownIdErrors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertTrue(
            "ExpressionVerifier skeleton should not produce UnknownIdentifier for declared identifiers; got: $unknownIdErrors",
            unknownIdErrors.isEmpty()
        )
        assertTrue(
            "Program with all declared identifiers and valid expressions should verify successfully; errors: ${result.errors}",
            result.isSuccessful
        )
    }

    /**
     * An undeclared IDENTIFIER in expression context (RHS of a typed var decl) produces
     * UnknownIdentifier(Context.EXPRESSION) — emitted by [Elaboration] per OQ-11.3=(a),
     * NOT by [ExpressionVerifier] (which returns emptyList() for IDENTIFIER in §4.1 skeleton).
     *
     * This confirms the OQ-11.3=(a) boundary: Elaboration owns expression-context IDENTIFIER
     * emission in §4.1; ExpressionVerifier gains this responsibility in §4.2.
     */
    @Test fun walkerReturnsEmptyListForIdentifierInSkeleton() {
        val module = parseAndAst("""
            module Skeleton {
                func f() {
                    int x = undeclaredName
                }
            }
        """.trimIndent())

        // Fix 4 (Seed A#1): Unit assertion — ExpressionVerifier IDENTIFIER case must return
        // emptyList() in §4.1 skeleton. Directly tests the skeleton's own output, not the
        // full pipeline. Verifies the OQ-11.3=(a) boundary: IDENTIFIER emission lives in
        // Elaboration, ExpressionVerifier returns emptyList() for IDENTIFIER in §4.1.
        val stmtData = module.functions[0].statements[0] as TypedVariableDeclarationAndAssignmentData
        val identifierExpr = stmtData.value  // ExpressionData(Kind.IDENTIFIER, "undeclaredName")
        assertEquals(
            "ExpressionVerifier.verifyExpression(IDENTIFIER, emptyScope) must return emptyList() in §4.1 skeleton",
            emptyList<VerifyError>(),
            ExpressionVerifier.verifyExpression(identifierExpr, SymbolTable())
        )

        // Full-pipeline assertion — the error IS emitted (from Elaboration), but NOT from ExpressionVerifier
        val result = Verifier.verifyModule(module, SymbolTable())
        assertFalse(
            "An undeclared identifier in expression context must be rejected",
            result.isSuccessful
        )
        val unknownIdErrors = result.errors.filterIsInstance<VerifyError.UnknownIdentifier>()
        assertTrue("Expected at least 1 UnknownIdentifier error from Elaboration", unknownIdErrors.isNotEmpty())
        val expressionContextError = unknownIdErrors.find {
            it.context == VerifyError.UnknownIdentifier.Context.EXPRESSION &&
            it.name == "undeclaredName"
        }
        assertFalse(
            "Expected UnknownIdentifier(EXPRESSION, 'undeclaredName') from Elaboration per OQ-11.3=(a)",
            expressionContextError == null
        )
    }
}
