package com.aaroncoplan.waterfall.compiler.statements.helpers;

public interface Translatable {
    VerificationResult verify();
    String translate();
}
