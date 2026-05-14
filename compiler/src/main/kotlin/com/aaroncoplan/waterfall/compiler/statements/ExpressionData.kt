package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser

class ExpressionData(filePath: String, ctx: WaterfallParser.ExpressionContext) {

    enum class Kind {
        NULL_LITERAL,
        BOOL_LITERAL,
        INT_LITERAL,
        DEC_LITERAL,
        STRING_LITERAL,
        IDENTIFIER,
        LAMBDA,
        BUNDLE,
        ARRAY,
        FUNCTION_CALL,
        BINARY_OP,
        ARRAY_INDEX,
        CAST,
    }

    @JvmField val kind: Kind
    /** NULL/INT/DEC/STRING/IDENTIFIER/BOOL */
    @JvmField val literalText: String?
    /** LAMBDA only */
    @JvmField val lambda: LambdaFunctionData?
    /** BUNDLE only */
    @JvmField val bundle: BundleLiteralData?
    /** ARRAY only */
    @JvmField val array: ArrayLiteralData?
    /** FUNCTION_CALL only */
    @JvmField val functionCall: FunctionCallData?
    /** BINARY_OP only: "and" / "or" / "equals" / "+" / "-" / ... */
    @JvmField val op: String?
    /** BINARY_OP only */
    @JvmField val left: ExpressionData?
    /** BINARY_OP only */
    @JvmField val right: ExpressionData?
    /** ARRAY_INDEX only */
    @JvmField val arrayIndex: ArrayIndexData?
    /** CAST only — the target type name */
    @JvmField val castTargetType: String?
    /** CAST only */
    @JvmField val castOperand: ExpressionData?

    init {
        // Cast alt — `expression CASTAS castTarget=type`. Check before binary because
        // it's also left-recursive and we want to identify it precisely.
        if (ctx.castTarget != null) {
            kind = Kind.CAST
            castTargetType = ctx.castTarget.text
            castOperand = ExpressionData(filePath, ctx.expression(0))
            op = null; left = null; right = null
            literalText = null
            lambda = null; bundle = null; array = null
            functionCall = null; arrayIndex = null
        }
        // Binary op alternative — must be checked next because the `op` label is the
        // only thing distinguishing it (all other accessors return null in that case).
        else if (ctx.op != null) {
            kind = Kind.BINARY_OP
            op = ctx.op.text
            left = ExpressionData(filePath, ctx.left)
            right = ExpressionData(filePath, ctx.right)
            literalText = null
            lambda = null; bundle = null; array = null
            functionCall = null; arrayIndex = null
            castTargetType = null; castOperand = null
        }
        else {
            // Leaf / primary alternatives.
            var k: Kind? = null
            var text: String? = null
            var ld: LambdaFunctionData? = null
            var bd: BundleLiteralData? = null
            var ad: ArrayLiteralData? = null
            var fc: FunctionCallData? = null
            var ai: ArrayIndexData? = null
            when {
                ctx.NULL() != null         -> { k = Kind.NULL_LITERAL;   text = ctx.NULL().text }
                ctx.BOOL_LITERAL() != null -> { k = Kind.BOOL_LITERAL;   text = ctx.BOOL_LITERAL().text }
                ctx.INT_LITERAL() != null  -> { k = Kind.INT_LITERAL;    text = ctx.INT_LITERAL().text }
                ctx.DEC_LITERAL() != null  -> { k = Kind.DEC_LITERAL;    text = ctx.DEC_LITERAL().text }
                ctx.STRING_LITERAL() != null -> { k = Kind.STRING_LITERAL; text = ctx.STRING_LITERAL().text }
                ctx.lambdaFunction() != null -> { k = Kind.LAMBDA;       ld = LambdaFunctionData(filePath, ctx.lambdaFunction()) }
                ctx.bundleLiteral() != null  -> { k = Kind.BUNDLE;       bd = BundleLiteralData(filePath, ctx.bundleLiteral()) }
                ctx.arrayLiteral() != null   -> { k = Kind.ARRAY;        ad = ArrayLiteralData(filePath, ctx.arrayLiteral()) }
                ctx.functionCall() != null   -> { k = Kind.FUNCTION_CALL; fc = FunctionCallData(filePath, ctx.functionCall()) }
                ctx.arrayIndex() != null     -> { k = Kind.ARRAY_INDEX;  ai = ArrayIndexData(filePath, ctx.arrayIndex()) }
                ctx.ID() != null             -> { k = Kind.IDENTIFIER;   text = ctx.ID().text }
                else -> throw RuntimeException("Unrecognized expression alternative")
            }
            kind = k!!
            literalText = text
            lambda = ld
            bundle = bd
            array = ad
            functionCall = fc
            arrayIndex = ai
            op = null
            left = null
            right = null
            castTargetType = null
            castOperand = null
        }
    }
}
