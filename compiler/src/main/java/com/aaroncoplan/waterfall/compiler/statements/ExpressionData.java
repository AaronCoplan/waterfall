package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;

public class ExpressionData {

    public enum Kind {
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

    public final Kind kind;
    public final String literalText;            // NULL/INT/DEC/STRING/IDENTIFIER
    public final LambdaFunctionData lambda;     // LAMBDA only
    public final BundleLiteralData bundle;      // BUNDLE only
    public final ArrayLiteralData array;        // ARRAY only
    public final FunctionCallData functionCall; // FUNCTION_CALL only
    public final String op;                     // BINARY_OP only: "and" / "or" / "equals"
    public final ExpressionData left;           // BINARY_OP only
    public final ExpressionData right;          // BINARY_OP only
    public final ArrayIndexData arrayIndex;     // ARRAY_INDEX only
    public final String castTargetType;         // CAST only — the target type name
    public final ExpressionData castOperand;    // CAST only

    public ExpressionData(String filePath, WaterfallParser.ExpressionContext ctx) {
        // Cast alt — `expression CASTAS castTarget=type`. Check before binary because
        // it's also left-recursive and we want to identify it precisely.
        if (ctx.castTarget != null) {
            this.kind = Kind.CAST;
            this.castTargetType = ctx.castTarget.getText();
            this.castOperand = new ExpressionData(filePath, ctx.expression(0));
            this.op = null; this.left = null; this.right = null;
            this.literalText = null;
            this.lambda = null; this.bundle = null; this.array = null;
            this.functionCall = null; this.arrayIndex = null;
            return;
        }
        // Binary op alternative — must be checked next because the `op` label is the
        // only thing distinguishing it (all other accessors return null in that case).
        if (ctx.op != null) {
            this.kind = Kind.BINARY_OP;
            this.op = ctx.op.getText();
            this.left = new ExpressionData(filePath, ctx.left);
            this.right = new ExpressionData(filePath, ctx.right);
            this.literalText = null;
            this.lambda = null; this.bundle = null; this.array = null;
            this.functionCall = null; this.arrayIndex = null;
            this.castTargetType = null; this.castOperand = null;
            return;
        }

        // Leaf / primary alternatives.
        Kind k;
        String text = null;
        LambdaFunctionData ld = null;
        BundleLiteralData bd = null;
        ArrayLiteralData ad = null;
        FunctionCallData fc = null;
        ArrayIndexData ai = null;
        if (ctx.NULL() != null) { k = Kind.NULL_LITERAL; text = ctx.NULL().getText(); }
        else if (ctx.BOOL_LITERAL() != null) { k = Kind.BOOL_LITERAL; text = ctx.BOOL_LITERAL().getText(); }
        else if (ctx.INT_LITERAL() != null) { k = Kind.INT_LITERAL; text = ctx.INT_LITERAL().getText(); }
        else if (ctx.DEC_LITERAL() != null) { k = Kind.DEC_LITERAL; text = ctx.DEC_LITERAL().getText(); }
        else if (ctx.STRING_LITERAL() != null) { k = Kind.STRING_LITERAL; text = ctx.STRING_LITERAL().getText(); }
        else if (ctx.lambdaFunction() != null) { k = Kind.LAMBDA; ld = new LambdaFunctionData(filePath, ctx.lambdaFunction()); }
        else if (ctx.bundleLiteral() != null) { k = Kind.BUNDLE; bd = new BundleLiteralData(filePath, ctx.bundleLiteral()); }
        else if (ctx.arrayLiteral() != null) { k = Kind.ARRAY; ad = new ArrayLiteralData(filePath, ctx.arrayLiteral()); }
        else if (ctx.functionCall() != null) { k = Kind.FUNCTION_CALL; fc = new FunctionCallData(filePath, ctx.functionCall()); }
        else if (ctx.arrayIndex() != null) { k = Kind.ARRAY_INDEX; ai = new ArrayIndexData(filePath, ctx.arrayIndex()); }
        else if (ctx.ID() != null) { k = Kind.IDENTIFIER; text = ctx.ID().getText(); }
        else throw new RuntimeException("Unrecognized expression alternative");

        this.kind = k;
        this.literalText = text;
        this.lambda = ld;
        this.bundle = bd;
        this.array = ad;
        this.functionCall = fc;
        this.arrayIndex = ai;
        this.op = null;
        this.left = null;
        this.right = null;
        this.castTargetType = null;
        this.castOperand = null;
    }
}
