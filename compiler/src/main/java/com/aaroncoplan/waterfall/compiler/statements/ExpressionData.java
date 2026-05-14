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
        BINARY_OP,
    }

    public final Kind kind;
    public final String literalText;            // NULL/INT/DEC/STRING/IDENTIFIER
    public final LambdaFunctionData lambda;     // LAMBDA only
    public final BundleLiteralData bundle;      // BUNDLE only
    public final ArrayLiteralData array;        // ARRAY only
    public final FunctionCallData functionCall; // FUNCTION_CALL only
    public final String op;                     // BINARY_OP only: "and" / "or" / "equals"
    public final ExpressionData left;           // BINARY_OP only
    public final ExpressionData right;          // BINARY_OP only

    public ExpressionData(String filePath, WaterfallParser.ExpressionContext ctx) {
        if (ctx.op != null) {
            this.kind = Kind.BINARY_OP;
            this.op = ctx.op.getText();
            this.left = new ExpressionData(filePath, ctx.left);
            this.right = new ExpressionData(filePath, ctx.right);
            this.literalText = null;
            this.lambda = null; this.bundle = null; this.array = null; this.functionCall = null;
            return;
        }
        // No-op default for binary fields when this is a leaf.
        this.op = null;
        this.left = null;
        this.right = null;
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
}
