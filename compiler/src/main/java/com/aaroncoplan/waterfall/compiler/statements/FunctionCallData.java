package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.parser.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionCallData {

    public enum Kind { LOCAL, MODULE, OBJECT }

    public final Kind kind;
    public final String moduleName;          // MODULE only
    public final List<String> receiverPath;  // OBJECT only — the dotted path before the function name
    public final String functionName;
    public final List<ExpressionData> positionalArguments;
    public final List<Pair<String, ExpressionData>> namedArguments;

    public FunctionCallData(String filePath, WaterfallParser.FunctionCallContext ctx) {
        if (ctx.localFunctionCall() != null) {
            WaterfallParser.LocalFunctionCallContext local = ctx.localFunctionCall();
            this.kind = Kind.LOCAL;
            this.moduleName = null;
            this.receiverPath = Collections.emptyList();
            this.functionName = local.functionName.getText();
            Pair<List<ExpressionData>, List<Pair<String, ExpressionData>>> args = extractArgs(filePath, local.functionCallArguments());
            this.positionalArguments = args.firstVal;
            this.namedArguments = args.secondVal;
        } else if (ctx.moduleFunctionCall() != null) {
            WaterfallParser.ModuleFunctionCallContext mod = ctx.moduleFunctionCall();
            this.kind = Kind.MODULE;
            this.moduleName = mod.moduleName.getText();
            this.receiverPath = Collections.emptyList();
            this.functionName = mod.functionName.getText();
            Pair<List<ExpressionData>, List<Pair<String, ExpressionData>>> args = extractArgs(filePath, mod.functionCallArguments());
            this.positionalArguments = args.firstVal;
            this.namedArguments = args.secondVal;
        } else if (ctx.objectFunctionCall() != null) {
            WaterfallParser.ObjectFunctionCallContext obj = ctx.objectFunctionCall();
            this.kind = Kind.OBJECT;
            this.moduleName = null;
            // Grammar: name=ID DOT (name=ID DOT)* functionName=ID L_PARENS ...
            // Every ID() except the last is part of the receiver path; the last is functionName.
            List<String> ids = obj.ID().stream().map(node -> node.getText()).collect(Collectors.toList());
            this.functionName = obj.functionName.getText();
            // Defensive: receiverPath = all IDs except the one matching functionName at the end.
            List<String> path = new ArrayList<>(ids);
            if (!path.isEmpty() && path.get(path.size() - 1).equals(this.functionName)) {
                path.remove(path.size() - 1);
            }
            this.receiverPath = Collections.unmodifiableList(path);
            Pair<List<ExpressionData>, List<Pair<String, ExpressionData>>> args = extractArgs(filePath, obj.functionCallArguments());
            this.positionalArguments = args.firstVal;
            this.namedArguments = args.secondVal;
        } else {
            throw new RuntimeException("Unrecognized functionCall variant");
        }
    }

    private static Pair<List<ExpressionData>, List<Pair<String, ExpressionData>>> extractArgs(
            String filePath, WaterfallParser.FunctionCallArgumentsContext args) {
        if (args == null) {
            return new Pair<>(Collections.emptyList(), Collections.emptyList());
        }
        if (args.namedArguments() != null) {
            List<Pair<String, ExpressionData>> named = args.namedArguments().namedArgument().stream()
                    .map(n -> new Pair<>(n.name.getText(), new ExpressionData(filePath, n.value)))
                    .collect(Collectors.toList());
            return new Pair<>(Collections.emptyList(), named);
        }
        if (args.positionalArguments != null) {
            List<ExpressionData> positional = args.positionalArguments.expression().stream()
                    .map(e -> new ExpressionData(filePath, e))
                    .collect(Collectors.toList());
            return new Pair<>(positional, Collections.emptyList());
        }
        return new Pair<>(Collections.emptyList(), Collections.emptyList());
    }

}
