package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.ExpressionData
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Expression-level verifier.
 *
 * **§4.1 skeleton**: dispatch structure ready for §4.2 full implementation.
 * All cases return [emptyList] until §4.2 wires per-expression source positions
 * ([ExpressionData.sourcePosition]) and full caller integration.
 *
 * **IDENTIFIER emission in §4.1** lives in [Elaboration] per OQ-11.3=(a), NOT here.
 * [Elaboration.elaborateExpression] emits [VerifyError.UnknownIdentifier] for
 * unresolved IDENTIFIER / ARRAY_INDEX / FUNCTION_CALL.LOCAL into the same error list
 * as [StatementVerifier]. This object gets real bodies in §4.2 (CAST, LAMBDA,
 * BINARY_OP, FUNCTION_CALL argument verification).
 *
 * @param expr the expression to verify
 * @param scope the enclosing symbol table
 * @param expectedType the type the expression should conform to (null = unconstrained)
 */
internal object ExpressionVerifier {

    fun verifyExpression(
        expr: ExpressionData,
        scope: SymbolTable,
        expectedType: WaterfallType? = null
    ): List<VerifyError> = when (expr.kind) {
        // §4.2: Cast target validation + operand verification
        ExpressionData.Kind.CAST             -> emptyList()
        // §4.2: Lambda parameter shadowing + body walk
        ExpressionData.Kind.LAMBDA           -> emptyList()
        // §4.2: Recurse into left + right
        ExpressionData.Kind.BINARY_OP        -> emptyList()
        // §4.2: Recurse into positional + named args (best-effort per OQ-11.1=a)
        ExpressionData.Kind.FUNCTION_CALL    -> emptyList()
        // §4.2: IDENTIFIER emission via ExpressionVerifier (§4.1: owned by Elaboration per OQ-11.3=(a))
        ExpressionData.Kind.IDENTIFIER       -> emptyList()
        // §4.2: Emit UnknownIdentifier for unresolved array target
        ExpressionData.Kind.ARRAY_INDEX      -> emptyList()
        // §4.2: Recurse into elements
        ExpressionData.Kind.ARRAY            -> emptyList()
        ExpressionData.Kind.BUNDLE           -> emptyList()
        // Literals: no sub-expressions, no type errors possible
        ExpressionData.Kind.NULL_LITERAL     -> emptyList()
        ExpressionData.Kind.BOOL_LITERAL     -> emptyList()
        ExpressionData.Kind.INT_LITERAL      -> emptyList()
        ExpressionData.Kind.DEC_LITERAL      -> emptyList()
        ExpressionData.Kind.STRING_LITERAL   -> emptyList()
    }
}
