package com.aaroncoplan.waterfall.compiler.statements.helpers;

import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;

public interface Translatable {
    VerificationResult verify(SymbolTable symbolTable);
    String translate();
}
