package com.aaroncoplan.waterfall.compiler.argumentparsing;

import java.util.List;

public class Arguments {

    private final List<String> files;
    private final String target;

    public Arguments(List<String> files, String target) {
        this.files = files;
        this.target = target;
    }

    public List<String> getFiles() {
        return files;
    }

    public String getTarget() {
        return target;
    }
}
