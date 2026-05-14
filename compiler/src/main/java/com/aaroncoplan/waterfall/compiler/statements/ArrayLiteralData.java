package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;

import java.util.List;
import java.util.stream.Collectors;

public class ArrayLiteralData {

    public final List<ExpressionData> elements;

    public ArrayLiteralData(String filePath, WaterfallParser.ArrayLiteralContext ctx) {
        this.elements = ctx.positionalArgumentList().expression().stream()
                .map(e -> new ExpressionData(filePath, e))
                .collect(Collectors.toList());
    }

}
