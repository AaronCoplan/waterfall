package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.DeclareResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolInfo
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolKind
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator
import com.aaroncoplan.waterfall.compiler.typesystem.PrimitiveTypes
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

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
        val result = symbolTable.declare(name, SymbolInfo(
            type = WaterfallType.fromSourceText(type),
            isReadonly = isImmutable(),
            kind = SymbolKind.Variable,
            sourcePosition = getSourcePosition()
        ))
        if (result is DeclareResult.Failure) {
            return VerificationResult(false, "Duplicate declaration: $name")
        }
        return VerificationResult(true, null)
    }

    override fun translate(backend: CodeGenerator): String = backend.emitTypedVarDecl(this)
}
