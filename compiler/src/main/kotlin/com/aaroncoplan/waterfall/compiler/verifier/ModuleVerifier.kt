package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.ExpressionData
import com.aaroncoplan.waterfall.compiler.statements.FunctionImplementationData
import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.symboltables.DeclareResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolInfo
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolKind
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.typesystem.PrimitiveTypes
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Two-pass module-level verifier. First pass: top-level variables (mirrors
 * today's Main.run loop order). Second pass: function declarations.
 *
 * [FunctionImplementationData] is NOT routed through [StatementVerifier] —
 * it introduces names into the module scope, so it lives here rather than
 * in the statement-level dispatcher. (Playbook §1 worked example: the
 * FunctionImplementationData routing trap.)
 */
internal object ModuleVerifier {

    fun verifyModule(module: ModuleAst, symbolTable: SymbolTable): VerifyResult {
        val errors = mutableListOf<VerifyError>()
        // F1=C: build the resolvedTypes side-table during the same scope walk.
        // IdentityHashMap ensures ExpressionData instances are keyed by object identity.
        val resolvedTypes = java.util.IdentityHashMap<ExpressionData, WaterfallType>()

        // Pass 1: top-level variables
        for (v in module.topLevelVariables) {
            errors += StatementVerifier.verifyStatement(v, symbolTable)
            Elaboration.elaborateStatement(v, symbolTable, resolvedTypes)
        }

        // Pass 2: function declarations
        for (f in module.functions) {
            errors += verifyFunctionDeclaration(f, symbolTable, resolvedTypes)
        }

        return VerifyResult(errors, resolvedTypes)
    }

    private fun verifyFunctionDeclaration(
        f: FunctionImplementationData,
        moduleScope: SymbolTable,
        resolvedTypes: java.util.IdentityHashMap<ExpressionData, WaterfallType>
    ): List<VerifyError> {
        val errors = mutableListOf<VerifyError>()

        // Validate return type
        if (f.returnType != null && !PrimitiveTypes.isPrimitiveOrArray(f.returnType)) {
            errors += VerifyError.UnknownType(f.returnType, f.getSourcePosition())
        }

        // Self-declaration into the module scope (enables recursion + duplicate detection)
        val selfResult = moduleScope.declare(f.name, SymbolInfo(
            type = WaterfallType.forReturnType(f.returnType),
            isReadonly = true,  // PITFALL #7: functions implicitly readonly
            kind = SymbolKind.Function(parameters = f.typedArguments.map {
                kotlin.Pair(it.secondVal, WaterfallType.fromSourceText(it.firstVal))
                //         ^^^^^^^^^^^^^                              ^^^^^^^^^^^^
                //         name first                                 type second
            }),
            sourcePosition = f.getSourcePosition()
        ))
        if (selfResult is DeclareResult.Failure) {
            errors += VerifyError.fromSymbolTable(selfResult.error, topLevel = true)
        }

        // Function body: enter a child scope, declare args, verify statements
        val functionScope = moduleScope.enterScope()
        for (arg in f.typedArguments) {
            if (!PrimitiveTypes.isPrimitiveOrArray(arg.firstVal)) {
                errors += VerifyError.UnknownType(arg.firstVal, f.getSourcePosition())
                continue
            }
            val argResult = functionScope.declare(arg.secondVal, SymbolInfo(
                type = WaterfallType.fromSourceText(arg.firstVal),
                isReadonly = false,
                kind = SymbolKind.Argument,
                sourcePosition = f.getSourcePosition()  // TODO(P10): per-arg positions — see PITFALL #8
            ))
            if (argResult is DeclareResult.Failure) {
                errors += VerifyError.fromSymbolTable(argResult.error)
            }
        }
        for (stmt in f.statements) {
            errors += StatementVerifier.verifyStatement(stmt, functionScope)
            Elaboration.elaborateStatement(stmt, functionScope, resolvedTypes)
        }
        moduleScope.exitScope(functionScope)

        return errors
    }
}
