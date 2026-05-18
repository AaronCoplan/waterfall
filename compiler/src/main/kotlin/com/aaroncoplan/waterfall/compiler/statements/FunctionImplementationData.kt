package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.StatementDispatcher
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.DeclareResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolInfo
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolKind
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator
import com.aaroncoplan.waterfall.compiler.typesystem.PrimitiveTypes
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType
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
        val selfResult = symbolTable.declare(name, SymbolInfo(
            type = WaterfallType.forReturnType(returnType),  // NOTE: forReturnType, NOT fromSourceText
            isReadonly = true,                                 // PITFALL #7: functions implicitly readonly
            kind = SymbolKind.Function(parameters = typedArguments.map {
                kotlin.Pair(it.secondVal, WaterfallType.fromSourceText(it.firstVal))
                //         ^^^^^^^^^^^^^                              ^^^^^^^^^^^^
                //         name first                                 type second
            }),
            sourcePosition = getSourcePosition()
        ))
        if (selfResult is DeclareResult.Failure) {
            return VerificationResult(false, "Duplicate top-level declaration: $name")
        }

        val functionSymbolTable = symbolTable.enterScope()
        for (arg in typedArguments) {
            if (!PrimitiveTypes.isPrimitiveOrArray(arg.firstVal)) {
                return VerificationResult(false,
                    "Illegal argument type '${arg.firstVal}' for arg ${arg.secondVal}. Known: ${PrimitiveTypes.ALL}")
            }
            val argResult = functionSymbolTable.declare(arg.secondVal, SymbolInfo(
                type = WaterfallType.fromSourceText(arg.firstVal),
                isReadonly = false,
                kind = SymbolKind.Argument,
                sourcePosition = getSourcePosition()  // TODO(P10): per-arg source positions blocked on typedArguments record migration — see PITFALL #8
            ))
            if (argResult is DeclareResult.Failure) {
                return VerificationResult(false, "Duplicate declaration: ${arg.secondVal}")
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
