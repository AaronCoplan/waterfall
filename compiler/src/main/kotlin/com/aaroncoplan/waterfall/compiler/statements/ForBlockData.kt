package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

class ForBlockData(filePath: String, ctx: WaterfallParser.ForBlockContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val iteratorName: String
    @JvmField val collectionName: String
    @JvmField val body: List<TranslatableStatement>

    init {
        // Grammar: FOR L_PARENS name=ID IN collection=ID R_PARENS ...
        // Both labels named name/collection -> two ID() tokens at positions 0 and 1.
        val inner = ctx.forInBlock()
        iteratorName = inner.ID(0).text
        collectionName = inner.ID(1).text
        body = StatementDispatcher.fromStatementBlock(filePath, inner.statementBlock())
    }

    override fun verify(symbolTable: SymbolTable): VerificationResult {
        // TODO(audit): iterator type isn't inferred from the collection yet.
        val scope = SymbolTable(symbolTable)
        for (s in body) {
            val r = s.verify(scope)
            if (!r.isSuccessful()) return r
        }
        return VerificationResult(true, null)
    }

    override fun translate(backend: CodeGenerator): String = backend.emitForBlock(this)
}
