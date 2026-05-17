package com.aaroncoplan.waterfall.compiler.symboltables

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Per-binding state stored in the symbol table. Replaces the `Any?` value at
 * `SymbolTable.kt:5` (audit D2).
 *
 * Immutable. To "change" a binding's readonly state without a parent-scope
 * write (the §2c shadow model), the SymbolTable uses markReadonlyLocal (added in §5.2);
 * to commit a flow-sensitive promotion at a branch join, it uses commitReadonly (also §5.2).
 * Both produce new SymbolInfo values via `.copy(isReadonly = true)` — the original
 * is never mutated in place. (KDoc cross-reference links are deliberately written as
 * plain prose here to avoid stale Dokka links during the §5.1 → §5.2 interim state.)
 *
 * Equality and hashCode are structural (data class). Two SymbolInfos are equal iff every
 * field is equal, including source position. This requires `SourcePosition` to also
 * provide structural equality — §5.1 converts `SourcePosition` to a data class (see §5.1
 * Files changed). Without that change, `.copy(isReadonly = true)` would still work (the
 * `sourcePosition` reference is shared), but two SymbolInfo values reconstructed from the
 * same source location via different ANTLR walks would compare unequal, silently breaking
 * test fixtures and any future invalidation logic.
 */
data class SymbolInfo(
    /** The declared (or inferred at P11+) type. For functions this is the
     *  return type; the parameter list lives on [kind]. */
    val type: WaterfallType,

    /**
     * Whether the binding is readonly. True iff the source declaration used
     * the `readonly` modifier (Piece 2 of this task), OR a flow-sensitive
     * `readonly x` statement has fired and the symbol table is exposing the
     * promoted state via markReadonlyLocal.
     *
     * This field reflects the binding as **currently observed via lookup**,
     * not a property of the original declaration. In a child scope's view of
     * an ancestor-owned binding, `isReadonly` may be `true` because of a
     * local shadow promotion even though the parent scope's stored entry has
     * `isReadonly = false`. See SymbolTable §2.4 for resolution semantics.
     */
    val isReadonly: Boolean,

    /** Variable / Argument / Function. Closes D6. */
    val kind: SymbolKind,

    /** Where this binding was declared. Used for friendly errors at P11+. */
    val sourcePosition: SourcePosition
)
