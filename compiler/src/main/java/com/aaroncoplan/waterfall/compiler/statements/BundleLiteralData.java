package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;

import java.util.List;
import java.util.stream.Collectors;

public class BundleLiteralData {

    public final List<ExpressionData> elements;

    public BundleLiteralData(String filePath, WaterfallParser.BundleLiteralContext ctx) {
        this.elements = ctx.positionalArgumentList().expression().stream()
                .map(e -> new ExpressionData(filePath, e))
                .collect(Collectors.toList());
    }

}
