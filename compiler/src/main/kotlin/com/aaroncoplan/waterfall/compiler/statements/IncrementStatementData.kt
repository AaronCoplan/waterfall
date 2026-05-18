package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

class IncrementStatementData(filePath: String, ctx: WaterfallParser.IncrementStatementContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text
    /** "++" or "--" */
    @JvmField val op: String = ctx.op.text

    override fun verify(symbolTable: SymbolTable): VerificationResult {
        val info = symbolTable.lookup(name)
        if (info != null && info.isReadonly) {
            return VerificationResult(false, "Cannot increment immutable binding '$name'")
        }
        return VerificationResult(true, null)
    }

    override fun translate(backend: CodeGenerator): String = backend.emitIncrementStatement(this)
}
