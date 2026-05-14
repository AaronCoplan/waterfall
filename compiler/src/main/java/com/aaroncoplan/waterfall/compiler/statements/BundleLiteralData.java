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

    public String translate() {
        // TODO(audit): bundle semantics aren't defined yet; legacy emitter renders the
        // source `|a, b|` literally. Per-target backends in later phases may diverge.
        return "|" + elements.stream().map(ExpressionData::translate).collect(Collectors.joining(", ")) + "|";
    }
}
