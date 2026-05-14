package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.DuplicateDeclarationException
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator
import com.aaroncoplan.waterfall.compiler.typesystem.PrimitiveTypes

class TypedVariableDeclarationAndAssignmentData(
    filePath: String,
    ctx: WaterfallParser.TypedVariableDeclarationAndAssignmentContext
) : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text
    @JvmField val type: String = ctx.type().text
    @JvmField val modifiers: List<String> =
        ctx.modifier()?.map { it.text } ?: emptyList()
    @JvmField val value: ExpressionData = ExpressionData(filePath, ctx.expression())

    fun isImmutable(): Boolean = "const" in modifiers || "imm" in modifiers

    override fun verify(symbolTable: SymbolTable): VerificationResult {
        if (!PrimitiveTypes.isPrimitiveOrArray(type)) {
            return VerificationResult(false,
                "Type '$type' is not a recognized primitive or primitive array. Known: ${PrimitiveTypes.ALL}")
        }
        try {
            symbolTable.declare(name, type)
        } catch (e: DuplicateDeclarationException) {
            return VerificationResult(false, "Duplicate declaration: $name")
        }
        return VerificationResult(true, null)
    }

    override fun translate(backend: CodeGenerator): String = backend.emitTypedVarDecl(this)
}
