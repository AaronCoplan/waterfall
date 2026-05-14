package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.parser.Pair;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LambdaFunctionData {

    public final List<Pair<String, String>> typedArguments;
    public final FunctionCallData body;  // null when the body is an empty `{}`

    public LambdaFunctionData(String filePath, WaterfallParser.LambdaFunctionContext ctx) {
        List<WaterfallParser.TypedArgumentContext> args = ctx.typedArgumentList() == null
                ? Collections.emptyList()
                : ctx.typedArgumentList().typedArgument();
        this.typedArguments = args.stream()
                .map(a -> new Pair<>(a.type().getText(), a.name.getText()))
                .collect(Collectors.toList());
        this.body = ctx.functionCall() == null ? null : new FunctionCallData(filePath, ctx.functionCall());
    }

    public String translate() {
        String argList = typedArguments.stream()
                .map(p -> p.firstVal + " " + p.secondVal)
                .collect(Collectors.joining(", "));
        String bodyText = body == null ? "{}" : body.translate();
        return String.format("(%s) ==> %s", argList, bodyText);
    }
}
