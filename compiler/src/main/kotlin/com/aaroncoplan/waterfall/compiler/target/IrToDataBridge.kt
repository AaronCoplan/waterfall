package com.aaroncoplan.waterfall.compiler.target

import com.aaroncoplan.waterfall.compiler.ir.IrExpression

/**
 * THROWAWAY: deleted when Python migrates (commit 3) and C migrates (commit 4).
 *
 * Shared backend-agnostic helpers for the temporary Python/C `lowerThenEmit` stubs.
 * Covers ONLY the 4 expression kinds that render identically in Python and C.
 * Backend-specific kinds (Bool, String, Null, BinaryOp, Cast, FunctionCall, Lambda,
 * ArrayLiteral, BundleLiteral) are inlined per backend.
 */
internal object IrToDataBridge {

    /**
     * Render generic-safe expression kinds. Throws for anything requiring
     * backend-specific logic — the backend must inline those cases.
     *
     * Safe cases: Identifier, IntLiteral, DecLiteral, ArrayIndex (and its index).
     */
    fun renderExpression(e: IrExpression): String = when (e) {
        is IrExpression.Identifier -> e.name   // R5: emit name; ignore IrType.Void
        is IrExpression.IntLiteral -> e.literalText
        is IrExpression.DecLiteral -> e.literalText
        is IrExpression.ArrayIndex -> "${e.target}[${renderExpression(e.index)}]"
        // Everything else is backend-specific — backend must inline
        else -> error(
            "IrToDataBridge.renderExpression: ${e::class.simpleName} is not generically " +
            "renderable; backend must inline this case in its lowerThenEmit stub"
        )
    }
}
