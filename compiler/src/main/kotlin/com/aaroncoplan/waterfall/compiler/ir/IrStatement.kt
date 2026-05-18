package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition

/**
 * Statement-level IR node. Every `*Data` class that subclasses TranslatableStatement
 * has an IR counterpart here (except ReadonlyPromotion which is P12-forward).
 *
 * No verify() or translate() here — verification lives in verifier/, backends
 * pattern-match on these variants after §5.5 migrates them.
 */
sealed class IrStatement {
    abstract val sourcePosition: SourcePosition

    data class TypedVarDecl(
        val name: String,
        val type: IrType,
        /** True iff source used `const`/`imm` (isImmutable() per §5.2 preservation). */
        val isReadonly: Boolean,
        val initializer: IrExpression,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class UntypedVarDecl(
        val name: String,
        val inferredType: IrType,
        val isReadonly: Boolean,
        val initializer: IrExpression,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class VarAssignment(
        val name: String,
        /** "=", "+=", "-=", "*=", "/=", "%=" — preserved from source. */
        val op: String,
        val value: IrExpression,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class IncrementStatement(
        val name: String,
        /** "++" or "--" */
        val op: String,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    /**
     * Form B `readonly x` statement (P12 — Piece 2). IR carries it for
     * stability; no P10 input produces it.
     */
    data class ReadonlyPromotion(
        val name: String,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class IfBlock(
        val ifBranch: Branch,
        val elifBranches: List<Branch>,
        val elseBody: List<IrStatement>?,
        override val sourcePosition: SourcePosition
    ) : IrStatement() {
        data class Branch(val condition: IrExpression, val body: List<IrStatement>)
    }

    data class WhileBlock(
        val condition: IrExpression,
        val body: List<IrStatement>,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class ForBlock(
        val iteratorName: String,
        val collectionName: String,
        val body: List<IrStatement>,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class ReturnStatement(
        val value: IrExpression?,        // null for bare `return`
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class FunctionCallStatement(
        val call: IrExpression.FunctionCall,
        override val sourcePosition: SourcePosition
    ) : IrStatement()
}
