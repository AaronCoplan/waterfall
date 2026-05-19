package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition

/**
 * Expression-level IR. Every variant carries its source position and its
 * inferred result type ([IrType]).
 *
 * Types are set by [IrLowering] reading from the [Elaboration] side-table
 * (F1=C resolution). P11 inference will update these with real inferred types.
 */
sealed class IrExpression {
    abstract val type: IrType
    abstract val sourcePosition: SourcePosition

    data class NullLiteral(
        override val type: IrType = IrType.Void,
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class BoolLiteral(
        val value: Boolean,
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        override val type: IrType = IrType.Bool
    }

    data class IntLiteral(
        val literalText: String,
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        override val type: IrType = IrType.Int
    }

    data class DecLiteral(
        val literalText: String,
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        override val type: IrType = IrType.Dec
    }

    data class StringLiteral(
        /** Raw source text including the backticks. Backends call StringLiteralText.unescape. */
        val literalText: String,
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        override val type: IrType = IrType.Char
    }

    data class Identifier(
        val name: String,
        override val type: IrType,
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class BinaryOp(
        val op: String,
        val left: IrExpression,
        val right: IrExpression,
        /** P10: left.type placeholder; P11 inference fills in proper result type. */
        override val type: IrType,
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class Cast(
        val targetType: IrType,
        val operand: IrExpression,
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        override val type: IrType = targetType
    }

    data class ArrayIndex(
        /** In P10, target is always a simple identifier (R3: no nested array-access). */
        val target: String,
        val index: IrExpression,
        override val type: IrType,
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class ArrayLiteral(
        val elements: List<IrExpression>,
        /** Element type from first element; IrType.Void if empty (Q3). */
        override val type: IrType,
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class BundleLiteral(
        /** Positional elements only — names dropped in P10 (R3). */
        val elements: List<IrExpression>,
        /** IrType.Void placeholder in P10. */
        override val type: IrType = IrType.Void,
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class Lambda(
        val parameters: List<IrParameter>,
        /** Null for empty body `{}`. */
        val body: FunctionCall?,
        /** IrType.Void placeholder in P10; lambdas have no first-class type until P11+. */
        override val type: IrType = IrType.Void,
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    /**
     * FunctionCall is BOTH an expression and (via [IrStatement.FunctionCallStatement])
     * a statement context. The IR class is shared.
     */
    data class FunctionCall(
        val kind: Kind,
        val moduleName: String?,           // non-null when kind = Module
        val receiverPath: List<String>,    // non-empty when kind = Object
        val functionName: String,
        val positionalArguments: List<IrExpression>,
        val namedArguments: List<Pair<String, IrExpression>>,
        /** Called function's return type (LOCAL); IrType.Void placeholder (MODULE/OBJECT). */
        override val type: IrType,
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        enum class Kind { Local, Module, Object }
    }
}
