package com.aaroncoplan.waterfall.compiler.statements.helpers;

public class SourcePosition {

    private final String fileName;
    private final int line, column;

    SourcePosition(String fileName, int line, int column) {
        this.fileName = fileName;
        this.line = line;
        this.column = column;
    }

    public String generateMessage() {
        return String.format("%s at %d:%d", fileName, line, column);
    }
}
