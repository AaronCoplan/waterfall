package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.ExpressionData
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Result of verifying a module. A non-empty `errors` list means the module
 * failed; an empty list means it succeeded.
 *
 * Unlike today's VerificationResult, errors accumulate. Verification of one
 * statement returning an error doesn't stop verification of subsequent
 * statements (the driver decides whether to bail). This is the P11+ friendly-
 * errors-with-multiple-issues precondition.
 *
 * NOTE: at the boundary, the existing `Main.run` aborts on the *first* error,
 * which is fine for P10 — but the verifier is structurally ready for the P11
 * "show all errors" upgrade. The driver change to accumulate-then-bail comes
 * with friendly errors at P11.
 *
 * **F1=C side-table:** [resolvedTypes] is the expression-type elaboration
 * side-table populated during the verify walk (§5.4). Keys are [ExpressionData]
 * instances by object identity (populated via `IdentityHashMap` in
 * [Elaboration]). [IrLowering.lowerModule] reads from this table instead of
 * re-walking the symbol table. Defaults to [emptyMap] so all §5.3 tests that
 * construct `VerifyResult(errors)` continue to compile without change.
 */
data class VerifyResult(
    val errors: List<VerifyError>,
    val resolvedTypes: Map<ExpressionData, WaterfallType> = emptyMap()
) {
    val isSuccessful: Boolean get() = errors.isEmpty()
}
