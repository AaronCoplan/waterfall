package com.aaroncoplan.waterfall.compiler.symboltables

import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * What kind of symbol this is. Distinguishes a function named `add` from a
 * variable named `add`, which today's symbol table cannot do (audit D6).
 *
 * P10 needs only Variable / Function / Argument — those are what the existing
 * compiler tracks. Records, sum types, and modules get their own variants when
 * the corresponding phases land (P12, P14).
 */
sealed class SymbolKind {
    /** A `int x = ...` or `x := ...` binding. */
    data object Variable : SymbolKind()

    /** A function parameter inside its function body. Distinct from Variable
     *  so the verifier can later print specialized error messages. */
    data object Argument : SymbolKind()

    /**
     * A top-level `func ... {}` declaration. The return type is also stored
     * on the SymbolInfo, but storing the parameter signature here lets a later
     * phase implement call-site type-checking without re-walking the AST.
     *
     * P10 doesn't yet enforce call-arg types — that's P11. But the data is
     * available from P10 onward.
     *
     * Parameter list. `Pair` is `kotlin.Pair` (not the custom
     * `com.aaroncoplan.waterfall.parser.Pair`). Field convention: **first = name,
     * second = type**. This ordering is the OPPOSITE of the legacy `(type, name)`
     * convention used in `FunctionImplementationData.typedArguments` and
     * `LambdaFunctionData` — when §5.2 migrates those callsites, swap the order
     * at the boundary. Per §5.1 → §5.2 contract, do NOT introduce a `TypedArgument`
     * data class or any additional field on this class in §5.1.
     */
    data class Function(
        val parameters: List<Pair<String, WaterfallType>>
    ) : SymbolKind()
}
