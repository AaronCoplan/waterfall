package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition
import com.aaroncoplan.waterfall.compiler.symboltables.DuplicateDeclarationError

/**
 * Structured error data for the verifier. Replaces today's
 * `VerificationResult.errorMessage: String?` with typed variants so:
 *   - friendly-error rendering (P11+) can format each kind differently
 *   - LSP (P13) can serialize them to JSON
 *   - tests can match on kind, not on string prefixes
 *
 * Each variant carries a [primaryPosition] for the offending node, plus
 * structured fields the renderer can format. The [message] is the canonical
 * short form used by [HumanRenderer]; see §4.8 for the byte-identical string
 * requirements.
 */
sealed class VerifyError {
    abstract val primaryPosition: SourcePosition
    abstract val message: String
    /** Stable error code for LSP/machine-readable consumers (§4.8 code table). */
    abstract val code: String

    // ------------------------------------------------------------------ //
    // WF1xxx — P10-era errors
    // ------------------------------------------------------------------ //

    /**
     * Write to an immutable/readonly binding (assignment or compound assignment).
     * Error code WF1001.
     *
     * Byte-identical string contract: [HumanRenderer] emits
     * `"Cannot assign to immutable binding '$name'"` — preserving the
     * §5.2-mandated substring that [ImmutableEnforcementTest] asserts on.
     */
    data class AssignToReadonly(
        val name: String,
        val declarationPosition: SourcePosition,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val code = "WF1001"
        override val message = "Cannot assign to immutable binding '$name'"
    }

    /**
     * Increment/decrement of an immutable/readonly binding. Error code WF1002.
     *
     * Byte-identical string contract: [HumanRenderer] emits
     * `"Cannot increment immutable binding '$name'"` — preserving the
     * §5.2-mandated substring that [ImmutableEnforcementTest] asserts on.
     */
    data class IncrementOfReadonly(
        val name: String,
        val declarationPosition: SourcePosition,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val code = "WF1002"
        override val message = "Cannot increment immutable binding '$name'"
    }

    /**
     * Form B promotion of a name not visible in scope (P12+). Error code WF1003.
     */
    data class ReadonlyOfUndeclared(
        val name: String,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val code = "WF1003"
        override val message = "Cannot freeze undeclared binding '$name'"
    }

    /**
     * Binding is already readonly — double-`readonly x` case (P12+). Error code WF1004.
     */
    data class AlreadyReadonly(
        val name: String,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val code = "WF1004"
        override val message = "Binding '$name' is already readonly"
    }

    /**
     * Duplicate binding name. Set [topLevel] = true when this is a function
     * self-declaration collision (so [HumanRenderer] can emit the
     * "Duplicate top-level declaration" form that existing tests depend on).
     * Error code WF1102.
     */
    data class DuplicateDeclaration(
        val name: String,
        val previousPosition: SourcePosition?,
        override val primaryPosition: SourcePosition,
        val topLevel: Boolean = false
    ) : VerifyError() {
        override val code = "WF1102"
        override val message: String = if (topLevel) {
            "Duplicate top-level declaration: $name"
        } else {
            "Duplicate declaration: $name"
        }
    }

    /**
     * Unknown / unrecognized type text. Error code WF1101.
     */
    /**
     * Unknown / unrecognized type text. Error code WF1101.
     *
     * Note: the `message` field is the short canonical form. [HumanRenderer]
     * appends the "Known: ..." suffix; consumers that need the verbose form
     * should go through the renderer, not the `message` field directly.
     */
    data class UnknownType(
        val typeText: String,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val code = "WF1101"
        override val message = "Type '$typeText' is not a recognized primitive or primitive array."
    }

    /**
     * `void` used as a value type (e.g., `void x = ...`). Error code WF1103.
     */
    data class VoidNotAValueType(
        val context: String,    // e.g. "variable declaration", "parameter type"
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val code = "WF1103"
        override val message = "Type 'void' is not a value type"
    }

    companion object {
        /**
         * Convert a symbol-table-level [DuplicateDeclarationError] into a
         * [DuplicateDeclaration] verifier error.
         *
         * Set [topLevel] = true ONLY for function self-declarations (where
         * [ModuleVerifier.verifyFunctionDeclaration] detects the collision at
         * module scope). Top-level variable duplicates use [topLevel] = false
         * (they're caught at the same module scope but are not function names).
         * All inner-scope duplicates use [topLevel] = false.
         */
        fun fromSymbolTable(
            e: DuplicateDeclarationError,
            topLevel: Boolean = false
        ): DuplicateDeclaration = DuplicateDeclaration(
            name = e.name,
            previousPosition = e.previouslyDeclaredAt,
            primaryPosition = e.attemptedAt,
            topLevel = topLevel
        )
    }
}
