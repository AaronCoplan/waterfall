package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.IfBlockData
import com.aaroncoplan.waterfall.compiler.statements.WhileBlockData
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable

/**
 * Branch-join readonly-intersection algorithm (§2d). P10 stub per OQ-1=C:
 *
 * - **Body verification (P10-final):** walks each branch body via
 *   [StatementVerifier.verifyStatement], entering/exiting child scopes.
 *   Errors from inside branches ARE collected and returned. The stub
 *   MUST walk bodies — a pure `return emptyList()` would silently swallow
 *   errors inside if/else/while/for branches (the SA-1 trap).
 *
 * - **Readonly intersection (TODO P12):** the `markReadonlyLocal` accumulation +
 *   shadow intersection + `commitReadonly` logic is deferred. No parser path
 *   to `markReadonlyLocal` exists in P10 (the `readonly x` statement is P12).
 *   Adding the intersection now would be dead code.
 */
internal object JoinAnalysis {

    private fun verifyBranch(
        body: List<TranslatableStatement>,
        parent: SymbolTable
    ): List<VerifyError> {
        val child = parent.enterScope()
        val errors = mutableListOf<VerifyError>()
        for (s in body) {
            errors += StatementVerifier.verifyStatement(s, child)
        }
        parent.exitScope(child)  // snapshot returned but not consumed yet (P12 intersection)
        return errors
    }

    /**
     * Verify an if/elif/else block. Branch bodies are walked for declare/
     * lookup/immutability errors. Readonly intersection is TODO(P12).
     *
     * **Asymmetry with [Elaboration]:** [Elaboration.elaborateStatement] for an
     * IfBlock walks the CONDITION expression (in parent scope) before entering each
     * branch child scope. This verifier does NOT walk the condition — condition
     * type-checking is P11 work. P11+ should evaluate the condition BEFORE entering
     * the child scope to keep verifier/elaboration scope walks aligned.
     *
     * PITFALL #11 (implicit-else note): in the full P12 implementation, an
     * absent `else` clause adds an implicit non-terminating empty-shadow
     * predecessor to the intersection. P10's stub doesn't run the intersection,
     * so the implicit-else path is irrelevant here — but document it so the
     * P12 implementer doesn't forget it.
     */
    fun verifyIfBlock(s: IfBlockData, scope: SymbolTable): List<VerifyError> {
        val errors = mutableListOf<VerifyError>()
        errors += verifyBranch(s.ifBranch.body, scope)
        for (elif in s.elifBranches) {
            errors += verifyBranch(elif.body, scope)
        }
        if (s.elseBody != null) {
            errors += verifyBranch(s.elseBody, scope)
        }
        // TODO(P12): compute intersection of non-terminating-branch shadow snapshots,
        //            then scope.commitReadonly(intersection). The `readonly x` statement
        //            does not exist in P10 grammar; intersection is dead code until P12.
        return errors
    }

    /**
     * Verify a while block. Loop body is walked for errors; readonly propagation
     * is moot (loops never propagate readonly past the body per PITFALL #9 + §2d).
     */
    fun verifyWhileBlock(s: WhileBlockData, scope: SymbolTable): List<VerifyError> {
        return verifyBranch(s.body, scope)
        // TODO(P12): loops never commitReadonly past the body (PITFALL #9).
        //            exitScope snapshot intentionally discarded in P10.
    }
}
