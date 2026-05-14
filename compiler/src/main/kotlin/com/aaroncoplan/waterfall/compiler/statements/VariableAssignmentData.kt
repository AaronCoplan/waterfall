package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

class VariableAssignmentData(filePath: String, ctx: WaterfallParser.VariableAssignmentContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text
    /** "=", "+=", "-=", "*=", "/=", or "%=" */
    @JvmField val op: String = ctx.op.text
    @JvmField val value: ExpressionData = ExpressionData(filePath, ctx.expression())

    override fun verify(symbolTable: SymbolTable): VerificationResult = VerificationResult(true, null)

    override fun translate(backend: CodeGenerator): String = backend.emitVarAssignment(this)
}
