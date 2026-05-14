package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

class FunctionCallStatementData(filePath: String, ctx: WaterfallParser.FunctionCallContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val call: FunctionCallData = FunctionCallData(filePath, ctx)

    override fun verify(symbolTable: SymbolTable): VerificationResult {
        // TODO(audit): no resolution against the symbol table yet.
        return VerificationResult(true, null)
    }

    override fun translate(backend: CodeGenerator): String = backend.emitFunctionCallStatement(this)
}
