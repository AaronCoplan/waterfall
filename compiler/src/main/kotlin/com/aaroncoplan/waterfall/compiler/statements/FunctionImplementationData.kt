package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.DuplicateDeclarationException
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator
import com.aaroncoplan.waterfall.compiler.typesystem.PrimitiveTypes
import com.aaroncoplan.waterfall.parser.Pair

class FunctionImplementationData(
    filePath: String,
    ctx: WaterfallParser.FunctionImplementationContext
) : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text
    @JvmField val returnType: String? = ctx.returnType?.text
    @JvmField val typedArguments: List<Pair<String, String>> =
        (ctx.typedArgumentList()?.typedArgument() ?: emptyList()).map { arg ->
            Pair(arg.type().text, arg.name.text)
        }
    @JvmField val statements: List<TranslatableStatement> =
        StatementDispatcher.fromStatementBlock(filePath, ctx.statementBlock())

    override fun verify(symbolTable: SymbolTable): VerificationResult {
        if (returnType != null && !PrimitiveTypes.isPrimitiveOrArray(returnType)) {
            return VerificationResult(false,
                "Illegal return type '$returnType'. Known: ${PrimitiveTypes.ALL}")
        }
        // Self-declaration into the outer (module) scope. Catches duplicate top-level
        // names; also makes the function visible to itself for recursion.
        try {
            symbolTable.declare(name, returnType ?: "void")
        } catch (e: DuplicateDeclarationException) {
            return VerificationResult(false, "Duplicate top-level declaration: $name")
        }

        val functionSymbolTable = SymbolTable(symbolTable)
        for (arg in typedArguments) {
            if (!PrimitiveTypes.isPrimitiveOrArray(arg.firstVal)) {
                return VerificationResult(false,
                    "Illegal argument type '${arg.firstVal}' for arg ${arg.secondVal}. Known: ${PrimitiveTypes.ALL}")
            }
            try {
                functionSymbolTable.declare(arg.secondVal, arg.firstVal)
            } catch (e: DuplicateDeclarationException) {
                return VerificationResult(false,
                    "Could not declare function arg ${arg.secondVal}, name already taken!")
            }
        }

        for (statement in statements) {
            val r = statement.verify(functionSymbolTable)
            if (!r.isSuccessful()) return r
        }
        return VerificationResult(true, null)
    }

    override fun translate(backend: CodeGenerator): String = backend.emitFunctionImpl(this)
}
