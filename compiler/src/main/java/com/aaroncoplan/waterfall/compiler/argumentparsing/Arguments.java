package com.aaroncoplan.waterfall.compiler.argumentparsing;

import java.util.List;

public class Arguments {

    private final List<String> files;

    public Arguments(List<String> files) {
        this.files = files;
    }

    public List<String> getFiles() {
        return files;
    }
}
