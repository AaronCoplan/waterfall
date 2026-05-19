package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.typesystem.PrimitiveTypes

/**
 * Human-readable renderer for [VerifyError]. Produces the same format as
 * today's `Main.kt:95` — `"$message in $primaryPosition.generateMessage()"`.
 *
 * **Byte-identical string contract (§5.2 + §5.3):**
 * - [VerifyError.AssignToReadonly] → `"Cannot assign to immutable binding '$name'"`
 * - [VerifyError.IncrementOfReadonly] → `"Cannot increment immutable binding '$name'"`
 * - [VerifyError.DuplicateDeclaration] (inner) → `"Duplicate declaration: $name"`
 * - [VerifyError.DuplicateDeclaration] (topLevel=true) → `"Duplicate top-level declaration: $name"`
 *
 * These strings preserve the substrings that [ImmutableEnforcementTest] and
 * [DuplicateInnerDeclarationTest] assert on. No test assertions change in §5.3.
 *
 * P13 will introduce a richer Elm/Roc-style renderer; this one stays as the
 * simple human-readable fallback for non-TTY/CI contexts.
 */
object HumanRenderer : ErrorRenderer {
    override fun render(error: VerifyError): String {
        val pos = error.primaryPosition.generateMessage()
        val msg = when (error) {
            is VerifyError.AssignToReadonly ->
                "Cannot assign to immutable binding '${error.name}'"
            is VerifyError.IncrementOfReadonly ->
                "Cannot increment immutable binding '${error.name}'"
            is VerifyError.DuplicateDeclaration ->
                if (error.topLevel) "Duplicate top-level declaration: ${error.name}"
                else "Duplicate declaration: ${error.name}"
            is VerifyError.UnknownType ->
                "Type '${error.typeText}' is not a recognized primitive or primitive array. Known: ${PrimitiveTypes.ALL}"
            is VerifyError.VoidNotAValueType ->
                "Type 'void' is not a value type"
            is VerifyError.ReadonlyOfUndeclared ->
                "Cannot freeze undeclared binding '${error.name}'"
            is VerifyError.AlreadyReadonly ->
                "Binding '${error.name}' is already readonly"
            // P11 §4.1 — WF12xx identifier-resolution errors
            is VerifyError.UnknownIdentifier ->
                error.message  // context-specific message lives on the variant (§2.2)
        }
        return "$msg in $pos"
    }
}
