package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * P10 minimal IR type. Same shape as [WaterfallType] — a wrapper that lets the
 * IR's representation evolve independently of the verifier's representation.
 *
 * After P11 grows type inference, IrType may carry richer info (e.g., source
 * expression position alongside the resolved type) that the verifier doesn't
 * need but the backends do.
 */
sealed class IrType {
    abstract fun render(): String
    abstract fun asWaterfallType(): WaterfallType

    data object Int : IrType() {
        override fun render() = "int"
        override fun asWaterfallType() = WaterfallType.IntType
    }
    data object Dec : IrType() {
        override fun render() = "dec"
        override fun asWaterfallType() = WaterfallType.DecType
    }
    data object Bool : IrType() {
        override fun render() = "bool"
        override fun asWaterfallType() = WaterfallType.BoolType
    }
    data object Char : IrType() {
        override fun render() = "char"
        override fun asWaterfallType() = WaterfallType.CharType
    }
    data class Array(val element: IrType) : IrType() {
        override fun render() = "${element.render()}[]"
        override fun asWaterfallType() = WaterfallType.ArrayType(element.asWaterfallType())
    }
    data object Void : IrType() {
        override fun render() = "void"
        override fun asWaterfallType() = WaterfallType.VoidType
    }

    companion object {
        fun fromWaterfallType(t: WaterfallType): IrType = when (t) {
            WaterfallType.IntType  -> Int
            WaterfallType.DecType  -> Dec
            WaterfallType.BoolType -> Bool
            WaterfallType.CharType -> Char
            WaterfallType.VoidType -> Void
            is WaterfallType.ArrayType -> Array(fromWaterfallType(t.element))
            is WaterfallType.ErrorType -> throw IllegalStateException(
                "Cannot lower error type to IR: ${t.sourceText}. " +
                "The verifier should have rejected this before lowering."
            )
        }
    }
}
