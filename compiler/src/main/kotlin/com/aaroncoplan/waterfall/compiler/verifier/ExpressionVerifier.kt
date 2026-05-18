package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.ExpressionData
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Expression-level verifier. P10 stub — expression type-checking and
 * identifier resolution are P11 work.
 *
 * P11 will fill in this object to: resolve identifier types via the
 * symbol table, check binary-op type compatibility, check function-call
 * argument types against the callee's parameter list.
 */
internal object ExpressionVerifier {

    /**
     * Verify an expression. P10 no-op — walks no sub-expressions.
     *
     * @param expr the expression to verify
     * @param scope the enclosing symbol table (for future identifier resolution)
     * @param expectedType the type the expression should conform to (null = unconstrained)
     */
    fun verifyExpression(
        expr: ExpressionData,
        scope: SymbolTable,
        expectedType: WaterfallType? = null
    ): List<VerifyError> =
        emptyList()  // TODO(P11): walk sub-expressions, check types, resolve identifiers
}
