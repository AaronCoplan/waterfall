package com.aaroncoplan.waterfall.compiler.statements.helpers;

import com.aaroncoplan.waterfall.parser.Pair;

public class VerificationResult extends Pair<Boolean, String> {

    public VerificationResult(boolean isSuccessful, String errorMessage) {
        super(isSuccessful, errorMessage);
    }

    public boolean isSuccessful() {
        return this.firstVal;
    }

    public String getErrorMessage() {
        return this.secondVal;
    }
}
