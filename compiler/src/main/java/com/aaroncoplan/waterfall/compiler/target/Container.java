package com.aaroncoplan.waterfall.compiler.target;

import java.util.ArrayList;
import java.util.List;

public class Container {

    private final List<String> headers, declarations, functions;

    public Container() {
        this.headers = new ArrayList<>();
        this.declarations = new ArrayList<>();
        this.functions = new ArrayList<>();
    }

    public void prependHeader(String header) {
        headers.add(0, header);
    }

    public void appendHeader(String header) {
        headers.add(header);
    }

    public void prependDeclaration(String declaration) {
        declarations.add(0, declaration);
    }

    public void appendDeclaration(String declaration) {
        declarations.add(declaration);
    }

    public void prependFunction(String function) {
        functions.add(0, function);
    }

    public void appendFunction(String function) {
        functions.add(function);
    }

    public String generate() {
        final String headerSpacing = headers.size() == 0 ? "" : "\n\n";
        final String declarationSpacing = declarations.size() == 0 ? "" : "\n\n";

        return String.join("\n", headers) + headerSpacing + String.join("\n", declarations) + declarationSpacing + String.join("\n", functions);
    }
}
