package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

class WhileBlockData(filePath: String, ctx: WaterfallParser.WhileBlockContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val condition: ExpressionData = ExpressionData(filePath, ctx.expression())
    @JvmField val body: List<TranslatableStatement> =
        StatementDispatcher.fromStatementBlock(filePath, ctx.statementBlock())

    override fun verify(symbolTable: SymbolTable): VerificationResult {
        val scope = SymbolTable(symbolTable)
        for (s in body) {
            val r = s.verify(scope)
            if (!r.isSuccessful()) return r
        }
        return VerificationResult(true, null)
    }

    override fun translate(backend: CodeGenerator): String = backend.emitWhileBlock(this)
}
