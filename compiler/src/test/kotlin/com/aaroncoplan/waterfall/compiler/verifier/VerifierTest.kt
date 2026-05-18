package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.parser.FileParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end verifier tests at module level (§4.7 spec-locked cases).
 *
 * **7 P10 surviving cases.** 8 cases from the original §4.7 are deferred to P12
 * because they require the `readonly x` statement (Form B promotion), which does not
 * exist in the P10 grammar. Those cases are documented in §4.7 under
 * "P12-deferred tests" and must NOT be added here until P12 lands the grammar rule.
 *
 * **Style:** pure JUnit 4 (`@Test` annotations, `Assert.*`). No Kotest dep.
 *
 * **parseAndAst helper:** parses source text in-memory via [FileParser.parseCodeString]
 * (no temp files needed). The parser accepts a nullable file path; we pass `"test.wf"`
 * so error positions are attributable if a test needs to inspect them in future.
 */
class VerifierTest {

    // ------------------------------------------------------------------ //
    // Test helper
    // ------------------------------------------------------------------ //

    /**
     * Parse Waterfall source text into a [ModuleAst] ready for [Verifier.verifyModule].
     *
     * Does NOT assert syntax-error-free parsing — tests here are written with valid
     * syntax by construction. If a source string has a typo that produces a parse error,
     * the resulting [ModuleAst] may be empty/partial, causing the verifier assertion to
     * fail with a misleading message. Use `parseResult.hasErrors()` to debug if needed.
     */
    private fun parseAndAst(source: String): ModuleAst {
        val parseResult = FileParser.parseCodeString("test.wf", source)
        return ModuleAst("test.wf", parseResult.getProgramAST().module())
    }

    // ------------------------------------------------------------------ //
    // §4.7 P10 spec-locked cases (7 surviving)
    // ------------------------------------------------------------------ //

    @Test fun emptyModule() {
        val module = parseAndAst("module Foo { }")
        val result = Verifier.verifyModule(module, SymbolTable())
        assertTrue(result.isSuccessful)
    }

    @Test fun duplicateTopLevelVarFails() {
        val module = parseAndAst("""
            module Foo {
                int x = 1
                int x = 2
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.DuplicateDeclaration)
    }

    @Test fun constAssignmentFails() {
        // Uses `const` modifier (isImmutable() = true) — exercises AssignToReadonly
        // without the P12 `readonly x` statement.
        val module = parseAndAst("""
            module Foo {
                func f() {
                    const int x = 1
                    x = 2
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.AssignToReadonly)
    }

    @Test fun constIncrementFails() {
        val module = parseAndAst("""
            module Foo {
                func f() {
                    const int x = 1
                    x++
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.IncrementOfReadonly)
    }

    @Test fun unknownTypeFails() {
        val module = parseAndAst("""
            module Foo {
                func f() {
                    unknown x = 1
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.UnknownType)
    }

    @Test fun duplicateInnerVarFails() {
        val module = parseAndAst("""
            module Foo {
                func go() {
                    int x = 1
                    int x = 2
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.DuplicateDeclaration)
    }

    @Test fun differentBranchesAllowSameVar() {
        // Each branch gets its own scope; same var name in sibling branches is fine.
        val module = parseAndAst("""
            module Foo {
                func go(int flag) {
                    if(flag) {
                        int x = 1
                    } else {
                        int x = 2
                    }
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertTrue(result.isSuccessful)
    }

    /**
     * Leg 3 catch (OQ-5.3-1): `void` as a variable type must be rejected.
     * WaterfallType.fromSourceText("void") returns VoidType (NOT ErrorType), so
     * it requires its own explicit check in verifyTypedVarDecl.
     */
    @Test fun voidTypedVarDeclFails() {
        val module = parseAndAst("""
            module Foo {
                func f() {
                    void x = 5
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.VoidNotAValueType)
    }

    /**
     * Leg 3 catch (OQ-5.3-4): defensive check — top-level void var also rejected.
     * Covers the top-level TypedVariableDeclarationAndAssignmentData path.
     */
    /**
     * C2: HumanRenderer distinguishes top-level vs. inner duplicate declarations.
     * Guards that the `topLevel` flag on [VerifyError.DuplicateDeclaration] flows
     * through HumanRenderer correctly.
     */
    @Test fun humanRendererDistinguishesTopLevelVsInnerDuplicateDeclaration() {
        val pos = com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition("t.wf", 1, 0)
        val inner = VerifyError.DuplicateDeclaration("x", null, pos, topLevel = false)
        val topLevel = VerifyError.DuplicateDeclaration("x", null, pos, topLevel = true)
        val innerMsg = HumanRenderer.render(inner)
        val topMsg = HumanRenderer.render(topLevel)
        assertTrue("inner should contain 'Duplicate declaration'", innerMsg.contains("Duplicate declaration"))
        assertTrue("inner should NOT contain 'top-level'", !innerMsg.contains("top-level"))
        assertTrue("top-level should contain 'Duplicate top-level declaration'", topMsg.contains("Duplicate top-level declaration"))
    }

    /**
     * C3: assigning to an undeclared LHS is a P10 no-op per OQ-3=C carry-forward.
     * §5.4 IrLowering will escalate on null-lookup. Regression guard: must NOT
     * produce an AssignToReadonly error (it's not readonly; it's not even declared).
     */
    @Test fun assignToUndeclaredLhsIsP10NoOp() {
        // OQ-3=C: verifyVarAssignment silently passes on null lookup — P11 closes this gap.
        val module = parseAndAst("""
            module Foo {
                func go() {
                    undeclaredVar = 5
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        // No AssignToReadonly error; no error at all from the verifier in P10.
        assertTrue(result.errors.none { it is VerifyError.AssignToReadonly })
    }

    @Test fun voidTopLevelVarDeclFails() {
        val module = parseAndAst("""
            module Foo {
                void x = 5
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.VoidNotAValueType)
    }
}
