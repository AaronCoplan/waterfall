package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.*
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.symboltables.DeclareResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolInfo
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolKind
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Verifies a single statement against the given scope. Mutates the scope
 * (declarations land here). Returns any errors produced.
 *
 * [FunctionImplementationData] is NOT dispatched from here — it is a
 * module-level declaration handled by [ModuleVerifier]. Encountering one
 * here is a dispatcher bug; the `else` branch throws.
 */
internal object StatementVerifier {

    fun verifyStatement(stmt: TranslatableStatement, scope: SymbolTable): List<VerifyError> {
        return when (stmt) {
            is TypedVariableDeclarationAndAssignmentData -> verifyTypedVarDecl(stmt, scope)
            is UntypedVariableDeclarationAndAssignmentData -> verifyUntypedVarDecl(stmt, scope)
            is VariableAssignmentData -> verifyVarAssignment(stmt, scope)
            is IncrementStatementData -> verifyIncrement(stmt, scope)
            // is ReadonlyPromotionData -> verifyReadonlyPromotion(stmt, scope)  // TODO(P12)
            is IfBlockData -> verifyIfBlock(stmt, scope)
            is WhileBlockData -> verifyWhileBlock(stmt, scope)
            is ForBlockData -> verifyForBlock(stmt, scope)
            is ReturnStatementData -> verifyReturn(stmt, scope)
            is FunctionCallStatementData -> verifyFunctionCallStatement(stmt, scope)
            // FunctionImplementationData is verified at module level by ModuleVerifier
            else -> error("Unexpected statement kind at verify-time: ${stmt::class.simpleName}")
        }
    }

    private fun verifyTypedVarDecl(
        s: TypedVariableDeclarationAndAssignmentData,
        scope: SymbolTable
    ): List<VerifyError> {
        val errors = mutableListOf<VerifyError>()
        val type = WaterfallType.fromSourceText(s.type)
        if (type is WaterfallType.ErrorType) {
            errors += VerifyError.UnknownType(s.type, s.getSourcePosition())
        }
        // FATAL-2 fix: use s.isImmutable() — checks `const`/`imm` modifiers.
        // Grammar still emits const/imm in P10; readonly unification is P12.
        val info = SymbolInfo(
            type = type,
            isReadonly = s.isImmutable(),
            kind = SymbolKind.Variable,
            sourcePosition = s.getSourcePosition()
        )
        val result = scope.declare(s.name, info)
        if (result is DeclareResult.Failure) {
            errors += VerifyError.fromSymbolTable(result.error)
        }
        return errors
    }

    private fun verifyUntypedVarDecl(
        s: UntypedVariableDeclarationAndAssignmentData,
        scope: SymbolTable
    ): List<VerifyError> {
        val type = WaterfallType.fromSourceText(s.inferredType)
        val info = SymbolInfo(
            type = type,
            isReadonly = s.isImmutable(),  // same isImmutable() preservation per §5.2
            kind = SymbolKind.Variable,
            sourcePosition = s.getSourcePosition()
        )
        val result = scope.declare(s.name, info)
        return if (result is DeclareResult.Failure) {
            listOf(VerifyError.fromSymbolTable(result.error))
        } else emptyList()
    }

    private fun verifyVarAssignment(
        s: VariableAssignmentData,
        scope: SymbolTable
    ): List<VerifyError> {
        val info = scope.lookup(s.name)
            ?: return emptyList()  // P10 doesn't error on unknown LHS — P11 closes this gap
        return if (info.isReadonly) {
            listOf(VerifyError.AssignToReadonly(
                name = s.name,
                declarationPosition = info.sourcePosition,
                primaryPosition = s.getSourcePosition()
            ))
        } else emptyList()
    }

    private fun verifyIncrement(
        s: IncrementStatementData,
        scope: SymbolTable
    ): List<VerifyError> {
        val info = scope.lookup(s.name)
            ?: return emptyList()
        return if (info.isReadonly) {
            listOf(VerifyError.IncrementOfReadonly(
                name = s.name,
                declarationPosition = info.sourcePosition,
                primaryPosition = s.getSourcePosition()
            ))
        } else emptyList()
    }

    private fun verifyIfBlock(s: IfBlockData, scope: SymbolTable): List<VerifyError> =
        JoinAnalysis.verifyIfBlock(s, scope)

    private fun verifyWhileBlock(s: WhileBlockData, scope: SymbolTable): List<VerifyError> =
        JoinAnalysis.verifyWhileBlock(s, scope)

    /**
     * For-loop body verification. Same scope-enter/exit pattern as while;
     * no readonly-intersection logic (loops never propagate readonly per PITFALL #9).
     * The iterator variable is not declared into scope here — ForBlockData doesn't
     * track its type in P10; P11 infers the element type from the collection.
     */
    internal fun verifyForBlock(s: ForBlockData, scope: SymbolTable): List<VerifyError> {
        val errors = mutableListOf<VerifyError>()
        val bodyScope = scope.enterScope()
        for (stmt in s.body) {
            errors += verifyStatement(stmt, bodyScope)
        }
        scope.exitScope(bodyScope)  // snapshot returned but not consumed (P12 join)
        return errors
    }

    /**
     * Return statement. P10 no-op — return-type checking is P11 work.
     */
    private fun verifyReturn(
        s: ReturnStatementData,
        scope: SymbolTable
    ): List<VerifyError> =
        emptyList()  // TODO(P11): check return expression type against enclosing function's return type

    /**
     * Function-call statement. P10 no-op — arg-type checking is P11 work.
     */
    private fun verifyFunctionCallStatement(
        s: FunctionCallStatementData,
        scope: SymbolTable
    ): List<VerifyError> =
        emptyList()  // TODO(P11): check argument types against callee's parameter list
}
