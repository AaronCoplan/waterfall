package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;

public class ArrayIndexData {

    public final String target;
    public final ExpressionData index;

    public ArrayIndexData(String filePath, WaterfallParser.ArrayIndexContext ctx) {
        this.target = ctx.target.getText();
        this.index = new ExpressionData(filePath, ctx.index);
    }
}
