package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.*
import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolInfo
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolKind
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Expression-type elaboration pass (F1=C side-table, §5.4).
 *
 * Populates a `MutableMap<ExpressionData, WaterfallType>` (keyed by object
 * identity via [java.util.IdentityHashMap]) during the same scope walk as
 * [StatementVerifier]. Every [ExpressionData] node reachable from a statement
 * gets an entry mapping it to its resolved [WaterfallType]. [IrLowering] reads
 * from this map instead of re-walking the symbol table, solving the
 * function-scope-lifetime gap (F1).
 *
 * ## Two-pass semantics (SA-2 resolution)
 *
 * Called from [ModuleVerifier] AFTER [StatementVerifier.verifyStatement] for
 * EACH body statement in the SAME scope context. Two separate passes per
 * statement; same scope instance. This means:
 *   - At function-body level: the scope already has all vars declared up to N
 *     when elaboration of statement N runs (verifier declared them in the same
 *     scope instance).
 *   - At NESTED levels (if/elif/else/while/for bodies): elaboration creates a
 *     FRESH child scope. To allow subsequent elaboration of the same body to
 *     resolve variables declared earlier in that body, this pass ALSO declares
 *     variables into the elaboration child scope as it walks. At function-body
 *     level the re-declare returns Failure (already there from verifier) — ignored.
 *
 * ## Lambda elaboration (SA-3 resolution)
 *
 * Lambda body elaboration is independent of the verifier (OQ-3=C: verifier
 * doesn't validate identifier resolution). [elaborateExpression] for
 * [ExpressionData.Kind.LAMBDA] sets up its own child scope, declares params,
 * and walks the body. Lambda-body identifier errors surface as [IrLowering]
 * throws at lowering time.
 */
internal object Elaboration {

    private val LAMBDA_POS = SourcePosition("lambda-expr", 0, 0)

    /**
     * Elaborate all [ExpressionData] nodes reachable from [stmt] within [scope].
     * Must be called with the same scope context that was active when
     * [StatementVerifier.verifyStatement] ran for this [stmt].
     */
    fun elaborateStatement(
        stmt: TranslatableStatement,
        scope: SymbolTable,
        table: MutableMap<ExpressionData, WaterfallType>
    ) {
        when (stmt) {
            is TypedVariableDeclarationAndAssignmentData -> {
                elaborateExpression(stmt.value, scope, table)
                // Declare into scope so subsequent stmts in the SAME body can look
                // up this var during elaboration. At top-level function scope the
                // verifier already declared it → DeclareResult.Failure, ignored.
                scope.declare(stmt.name, SymbolInfo(
                    type = WaterfallType.fromSourceText(stmt.type),
                    isReadonly = stmt.isImmutable(),
                    kind = SymbolKind.Variable,
                    sourcePosition = stmt.getSourcePosition()
                ))
            }

            is UntypedVariableDeclarationAndAssignmentData -> {
                elaborateExpression(stmt.value, scope, table)
                scope.declare(stmt.name, SymbolInfo(
                    type = WaterfallType.fromSourceText(stmt.inferredType),
                    isReadonly = stmt.isImmutable(),
                    kind = SymbolKind.Variable,
                    sourcePosition = stmt.getSourcePosition()
                ))
            }

            is VariableAssignmentData ->
                elaborateExpression(stmt.value, scope, table)

            is IncrementStatementData -> { /* no expression sub-tree */ }

            is IfBlockData -> {
                elaborateExpression(stmt.ifBranch.condition, scope, table)
                val thenScope = scope.enterScope()
                stmt.ifBranch.body.forEach { elaborateStatement(it, thenScope, table) }
                scope.exitScope(thenScope)
                for (elif in stmt.elifBranches) {
                    elaborateExpression(elif.condition, scope, table)
                    val elifScope = scope.enterScope()
                    elif.body.forEach { elaborateStatement(it, elifScope, table) }
                    scope.exitScope(elifScope)
                }
                if (stmt.elseBody != null) {
                    val elseScope = scope.enterScope()
                    stmt.elseBody.forEach { elaborateStatement(it, elseScope, table) }
                    scope.exitScope(elseScope)
                }
            }

            is WhileBlockData -> {
                elaborateExpression(stmt.condition, scope, table)
                val bodyScope = scope.enterScope()
                stmt.body.forEach { elaborateStatement(it, bodyScope, table) }
                scope.exitScope(bodyScope)
            }

            is ForBlockData -> {
                val bodyScope = scope.enterScope()
                // Declare iterator so body expressions can resolve it
                bodyScope.declare(stmt.iteratorName, SymbolInfo(
                    type = WaterfallType.IntType,
                    isReadonly = false,
                    kind = SymbolKind.Argument,
                    sourcePosition = stmt.getSourcePosition()
                ))
                stmt.body.forEach { elaborateStatement(it, bodyScope, table) }
                scope.exitScope(bodyScope)
            }

            is ReturnStatementData ->
                stmt.value?.let { elaborateExpression(it, scope, table) }

            is FunctionCallStatementData ->
                elaborateFunctionCall(stmt.call, scope, table)

            else -> { /* FunctionImplementationData handled at module level */ }
        }
    }

    /**
     * Elaborate a single [ExpressionData] node and all its sub-expressions.
     * Writes the resolved [WaterfallType] for this node into [table].
     */
    fun elaborateExpression(
        expr: ExpressionData,
        scope: SymbolTable,
        table: MutableMap<ExpressionData, WaterfallType>
    ) {
        val resolvedType: WaterfallType = when (expr.kind) {

            // Scope-dependent — need symbol table
            ExpressionData.Kind.IDENTIFIER -> {
                val name = expr.literalText ?: return
                scope.lookup(name)?.type ?: WaterfallType.VoidType
                // null → IrLowering will throw (OQ-3=C): undeclared identifier
            }

            ExpressionData.Kind.FUNCTION_CALL -> {
                val fc = expr.functionCall ?: return
                elaborateFunctionCall(fc, scope, table)
                if (fc.kind == FunctionCallData.Kind.LOCAL) {
                    scope.lookup(fc.functionName)?.type ?: WaterfallType.VoidType
                } else {
                    WaterfallType.VoidType  // MODULE/OBJECT: Void placeholder (R3)
                }
            }

            ExpressionData.Kind.ARRAY_INDEX -> {
                val ai = expr.arrayIndex ?: return
                elaborateExpression(ai.index, scope, table)
                val arrayType = scope.lookup(ai.target)?.type
                if (arrayType is WaterfallType.ArrayType) arrayType.element
                else WaterfallType.VoidType  // target not in scope → Void placeholder
            }

            // Constant-type — no scope lookup
            ExpressionData.Kind.NULL_LITERAL     -> WaterfallType.VoidType
            ExpressionData.Kind.BOOL_LITERAL     -> WaterfallType.BoolType
            ExpressionData.Kind.INT_LITERAL      -> WaterfallType.IntType
            ExpressionData.Kind.DEC_LITERAL      -> WaterfallType.DecType
            ExpressionData.Kind.STRING_LITERAL   -> WaterfallType.CharType

            ExpressionData.Kind.CAST -> {
                expr.castOperand?.let { elaborateExpression(it, scope, table) }
                val targetText = expr.castTargetType ?: return
                WaterfallType.fromSourceText(targetText)
            }

            ExpressionData.Kind.BINARY_OP -> {
                val l = expr.left ?: return
                val r = expr.right ?: return
                elaborateExpression(l, scope, table)
                elaborateExpression(r, scope, table)
                table[l] ?: WaterfallType.IntType  // left.type placeholder; P11 fills
            }

            ExpressionData.Kind.ARRAY -> {
                val elements = expr.array?.elements ?: emptyList()
                elements.forEach { elaborateExpression(it, scope, table) }
                if (elements.isEmpty()) {
                    WaterfallType.VoidType  // Q3: empty array → Void placeholder
                } else {
                    table[elements[0]] ?: WaterfallType.VoidType
                }
            }

            ExpressionData.Kind.BUNDLE -> {
                val elements = expr.bundle?.elements ?: emptyList()
                elements.forEach { elaborateExpression(it, scope, table) }
                WaterfallType.VoidType  // P10 placeholder (R3)
            }

            ExpressionData.Kind.LAMBDA -> {
                val lam = expr.lambda ?: return
                val lambdaScope = scope.enterScope()
                // Declare lambda params into child scope (SA-3: independent of verifier)
                // Legacy ordering: firstVal=type, secondVal=name
                for (arg in lam.typedArguments) {
                    lambdaScope.declare(arg.secondVal, SymbolInfo(
                        type = WaterfallType.fromSourceText(arg.firstVal),
                        isReadonly = false,
                        kind = SymbolKind.Argument,
                        sourcePosition = LAMBDA_POS
                    ))
                }
                lam.body?.let { elaborateFunctionCall(it, lambdaScope, table) }
                scope.exitScope(lambdaScope)
                WaterfallType.VoidType  // Lambda type = Void placeholder
            }
        }
        table[expr] = resolvedType
    }

    private fun elaborateFunctionCall(
        fc: FunctionCallData,
        scope: SymbolTable,
        table: MutableMap<ExpressionData, WaterfallType>
    ) {
        fc.positionalArguments.forEach { elaborateExpression(it, scope, table) }
        // namedArguments is List<com.aaroncoplan.waterfall.parser.Pair<String, ExpressionData>>
        // — use .secondVal (not destructuring; the custom Pair has no component functions)
        fc.namedArguments.forEach { elaborateExpression(it.secondVal, scope, table) }
    }
}
