package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

class IfBlockData(filePath: String, ctx: WaterfallParser.IfBlockContext)
    : TranslatableStatement(filePath, ctx) {

    class Branch(
        @JvmField val condition: ExpressionData,
        @JvmField val body: List<TranslatableStatement>
    )

    @JvmField val ifBranch: Branch = Branch(
        ExpressionData(filePath, ctx.expression()),
        StatementDispatcher.fromStatementBlock(filePath, ctx.statementBlock())
    )

    @JvmField val elifBranches: List<Branch> = (ctx.elifBlock() ?: emptyList()).map { eb ->
        Branch(
            ExpressionData(filePath, eb.expression()),
            StatementDispatcher.fromStatementBlock(filePath, eb.statementBlock())
        )
    }

    /** Null when there's no `else` clause. */
    @JvmField val elseBody: List<TranslatableStatement>? = ctx.elseBlock()?.let {
        StatementDispatcher.fromStatementBlock(filePath, it.statementBlock())
    }

    override fun verify(symbolTable: SymbolTable): VerificationResult {
        // TODO(audit): condition is not type-checked; phase 5+ should require bool.
        val scope = SymbolTable(symbolTable)
        for (s in ifBranch.body) {
            val r = s.verify(scope)
            if (!r.isSuccessful()) return r
        }
        for (elif in elifBranches) {
            val elifScope = SymbolTable(symbolTable)
            for (s in elif.body) {
                val r = s.verify(elifScope)
                if (!r.isSuccessful()) return r
            }
        }
        if (elseBody != null) {
            val elseScope = SymbolTable(symbolTable)
            for (s in elseBody) {
                val r = s.verify(elseScope)
                if (!r.isSuccessful()) return r
            }
        }
        return VerificationResult(true, null)
    }

    override fun translate(backend: CodeGenerator): String = backend.emitIfBlock(this)
}
