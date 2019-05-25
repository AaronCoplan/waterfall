package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.parser.Pair;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionImplementationData extends TranslatableStatement {
    public final String name, returnType;
    public final List<Pair<String, String>> typedArguments;

    public FunctionImplementationData(String filePath, WaterfallParser.FunctionImplementationContext functionImplementationContext) {
        super(filePath, functionImplementationContext);
        this.name = functionImplementationContext.name.getText();
        this.returnType = functionImplementationContext.returnType == null ? null : functionImplementationContext.returnType.getText();
        List<WaterfallParser.TypedArgumentContext> typedArgumentsContext = functionImplementationContext.typedArgumentList() == null ? Collections.emptyList() : functionImplementationContext.typedArgumentList().typedArgument();
        this.typedArguments = typedArgumentsContext.stream().map(arg -> new Pair<>(arg.type().getText(), arg.name.getText())).collect(Collectors.toList());
    }
}
