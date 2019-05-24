package com.aaroncoplan.waterfall.compiler.helpers;

import com.aaroncoplan.waterfall.parser.Pair;

import java.util.List;

public class FunctionImplementationData {
    public final String name, returnType;
    public final List<Pair<String, String>> typedArguments;

    FunctionImplementationData(String name, String returnType, List<Pair<String, String>> typedArguments) {
        this.name = name;
        this.returnType = returnType;
        this.typedArguments = typedArguments;
    }
}
