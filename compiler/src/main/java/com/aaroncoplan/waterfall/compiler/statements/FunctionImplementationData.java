package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
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

    @Override
    public VerificationResult verify() {
        if(returnType != null && !"int".equals(returnType)) {
            return new VerificationResult(false, "Illegal return type " + returnType);
        }
        for(Pair<String, String> arg : typedArguments) {
            if(!"int".equals(arg.firstVal)) {
                return new VerificationResult(false, "Illegal argument type " + arg.firstVal + " for arg " + arg.secondVal);
            }
        }
        return new VerificationResult(true, null);
    }

    @Override
    public String translate() {
        final String translatedReturnType = returnType == null ? "void" : returnType;
        final String args = String.join(", ", typedArguments.stream().map(arg -> String.format("%s %s", arg.firstVal, arg.secondVal)).collect(Collectors.toList()));
        return String.format("%s %s(%s) {}", translatedReturnType, name, args);
    }
}
