package com.aaroncoplan.waterfall.compiler.statements;

import com.aaroncoplan.waterfall.generated.WaterfallParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates a module's top-level declarations after parsing. Each backend is given
 * one of these and decides how to render the module as a whole (e.g. namespace
 * wrapping, header comments, output ordering).
 */
public class ModuleAst {

    public final String name;
    public final List<TypedVariableDeclarationAndAssignmentData> topLevelVariables;
    public final List<FunctionImplementationData> functions;

    public ModuleAst(String filePath, WaterfallParser.ModuleContext ctx) {
        this.name = ctx.name.getText();
        List<TypedVariableDeclarationAndAssignmentData> vars = new ArrayList<>();
        List<FunctionImplementationData> fns = new ArrayList<>();
        for (WaterfallParser.TopLevelDeclarationContext tld : ctx.topLevelDeclaration()) {
            if (tld.typedVariableDeclarationAndAssignment() != null) {
                vars.add(new TypedVariableDeclarationAndAssignmentData(filePath, tld.typedVariableDeclarationAndAssignment()));
            } else if (tld.functionImplementation() != null) {
                fns.add(new FunctionImplementationData(filePath, tld.functionImplementation()));
            }
        }
        this.topLevelVariables = Collections.unmodifiableList(vars);
        this.functions = Collections.unmodifiableList(fns);
    }
}
