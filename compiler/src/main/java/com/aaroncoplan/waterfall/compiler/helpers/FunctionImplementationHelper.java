package com.aaroncoplan.waterfall.compiler.helpers;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.parser.Pair;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionImplementationHelper {

    public static FunctionImplementationData extractData(WaterfallParser.FunctionImplementationContext functionImplementation) {
        String functionName = functionImplementation.name.getText();
        String returnType = functionImplementation.returnType == null ? null : functionImplementation.returnType.getText();
        List<WaterfallParser.TypedArgumentContext> typedArgumentsContext = functionImplementation.typedArgumentList() == null ? Collections.emptyList() : functionImplementation.typedArgumentList().typedArgument();
        List<Pair<String, String>> typedArguments = typedArgumentsContext.stream().map(arg -> new Pair<>(arg.type().getText(), arg.name.getText())).collect(Collectors.toList());
        return new FunctionImplementationData(functionName, returnType, typedArguments);
    }

}


