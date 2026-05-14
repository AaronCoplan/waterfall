package com.aaroncoplan.waterfall.compiler.statements.helpers

import com.aaroncoplan.waterfall.parser.Pair

class VerificationResult(isSuccessful: Boolean, errorMessage: String?)
    : Pair<Boolean, String?>(isSuccessful, errorMessage) {

    fun isSuccessful(): Boolean = firstVal
    fun getErrorMessage(): String? = secondVal
}
