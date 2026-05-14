package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;

public class ExpressionData {

    public enum Kind {
        NULL_LITERAL,
        INT_LITERAL,
        DEC_LITERAL,
        STRING_LITERAL,
        IDENTIFIER,
        LAMBDA,
        BUNDLE,
        ARRAY,
        FUNCTION_CALL,
    }

    public final Kind kind;
    public final String literalText;          // for NULL/INT/DEC/STRING/IDENTIFIER
    public final LambdaFunctionData lambda;   // LAMBDA only
    public final BundleLiteralData bundle;    // BUNDLE only
    public final ArrayLiteralData array;      // ARRAY only
    public final FunctionCallData functionCall; // FUNCTION_CALL only

    public ExpressionData(String filePath, WaterfallParser.ExpressionContext ctx) {
        if (ctx.NULL() != null) {
            this.kind = Kind.NULL_LITERAL;
            this.literalText = ctx.NULL().getText();
            this.lambda = null; this.bundle = null; this.array = null; this.functionCall = null;
        } else if (ctx.INT_LITERAL() != null) {
            this.kind = Kind.INT_LITERAL;
            this.literalText = ctx.INT_LITERAL().getText();
            this.lambda = null; this.bundle = null; this.array = null; this.functionCall = null;
        } else if (ctx.DEC_LITERAL() != null) {
            this.kind = Kind.DEC_LITERAL;
            this.literalText = ctx.DEC_LITERAL().getText();
            this.lambda = null; this.bundle = null; this.array = null; this.functionCall = null;
        } else if (ctx.STRING_LITERAL() != null) {
            this.kind = Kind.STRING_LITERAL;
            this.literalText = ctx.STRING_LITERAL().getText();
            this.lambda = null; this.bundle = null; this.array = null; this.functionCall = null;
        } else if (ctx.lambdaFunction() != null) {
            this.kind = Kind.LAMBDA;
            this.literalText = null;
            this.lambda = new LambdaFunctionData(filePath, ctx.lambdaFunction());
            this.bundle = null; this.array = null; this.functionCall = null;
        } else if (ctx.bundleLiteral() != null) {
            this.kind = Kind.BUNDLE;
            this.literalText = null;
            this.bundle = new BundleLiteralData(filePath, ctx.bundleLiteral());
            this.lambda = null; this.array = null; this.functionCall = null;
        } else if (ctx.arrayLiteral() != null) {
            this.kind = Kind.ARRAY;
            this.literalText = null;
            this.array = new ArrayLiteralData(filePath, ctx.arrayLiteral());
            this.lambda = null; this.bundle = null; this.functionCall = null;
        } else if (ctx.functionCall() != null) {
            this.kind = Kind.FUNCTION_CALL;
            this.literalText = null;
            this.functionCall = new FunctionCallData(filePath, ctx.functionCall());
            this.lambda = null; this.bundle = null; this.array = null;
        } else if (ctx.ID() != null) {
            this.kind = Kind.IDENTIFIER;
            this.literalText = ctx.ID().getText();
            this.lambda = null; this.bundle = null; this.array = null; this.functionCall = null;
        } else {
            throw new RuntimeException("Unrecognized expression alternative");
        }
    }

    public String translate() {
        switch (kind) {
            case NULL_LITERAL:
                // TODO(audit): "null" maps to legacy emitter literal NULL; backends will reshape.
                return "NULL";
            case INT_LITERAL:
            case DEC_LITERAL:
            case IDENTIFIER:
                return literalText;
            case STRING_LITERAL:
                // Source string literals are backtick-delimited; legacy emitter keeps backticks.
                // Per-target backends will translate to "..." (C/Python) or template literals (JS).
                return literalText;
            case LAMBDA:
                return lambda.translate();
            case BUNDLE:
                return bundle.translate();
            case ARRAY:
                return array.translate();
            case FUNCTION_CALL:
                return functionCall.translate();
            default:
                throw new RuntimeException("Unrecognized kind " + kind);
        }
    }
}
