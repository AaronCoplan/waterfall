package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.DeclareResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolInfo
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolKind
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

class UntypedVariableDeclarationAndAssignmentData(
    filePath: String,
    ctx: WaterfallParser.UntypedVariableDeclarationAndAssignmentContext
) : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text
    @JvmField val modifiers: List<String> =
        ctx.modifier()?.map { it.text } ?: emptyList()
    @JvmField val value: ExpressionData = ExpressionData(filePath, ctx.expression())
    @JvmField val inferredType: String = inferType(value)

    fun isImmutable(): Boolean = "const" in modifiers || "imm" in modifiers

    override fun verify(symbolTable: SymbolTable): VerificationResult {
        val result = symbolTable.declare(name, SymbolInfo(
            type = WaterfallType.fromSourceText(inferredType),
            isReadonly = isImmutable(),
            kind = SymbolKind.Variable,
            sourcePosition = getSourcePosition()
        ))
        if (result is DeclareResult.Failure) {
            return VerificationResult(false, "Duplicate declaration: $name")
        }
        return VerificationResult(true, null)
    }

    override fun translate(backend: CodeGenerator): String = backend.emitUntypedVarDecl(this)

    companion object {
        private fun inferType(expr: ExpressionData): String = when (expr.kind) {
            ExpressionData.Kind.INT_LITERAL -> "int"
            ExpressionData.Kind.DEC_LITERAL -> "dec"
            // TODO(audit): no first-class string type yet; defaults to char so the
            // C backend can emit `char *`. JS/Python don't care.
            ExpressionData.Kind.STRING_LITERAL -> "char"
            // TODO(audit): cross-expression type inference (identifiers, calls,
            // arithmetic) not implemented; default int.
            else -> "int"
        }
    }
}
