package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.*
import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolInfo
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolKind
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType
import com.aaroncoplan.waterfall.compiler.verifier.VerifyError.UnknownIdentifier

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
     *
     * **OQ-11.3=(a) §4.1**: [errors] receives [UnknownIdentifier] for unresolved
     * expression-context identifiers (IDENTIFIER, ARRAY_INDEX target, FUNCTION_CALL.LOCAL).
     * The same [errors] list that [StatementVerifier.verifyStatement] writes into is passed
     * here so verifier + elaboration errors accumulate in one place (see [ModuleVerifier]).
     */
    fun elaborateStatement(
        stmt: TranslatableStatement,
        scope: SymbolTable,
        table: MutableMap<ExpressionData, WaterfallType>,
        errors: MutableList<VerifyError>
    ) {
        val stmtPos = stmt.getSourcePosition()
        when (stmt) {
            is TypedVariableDeclarationAndAssignmentData -> {
                elaborateExpression(stmt.value, scope, table, errors, stmtPos)
                // Declare into scope so subsequent stmts in the SAME body can look
                // up this var during elaboration. At top-level function scope the
                // verifier already declared it → DeclareResult.Failure, ignored.
                scope.declare(stmt.name, SymbolInfo(
                    type = WaterfallType.fromSourceText(stmt.type),
                    isReadonly = stmt.isImmutable(),
                    kind = SymbolKind.Variable,
                    sourcePosition = stmtPos
                ))
            }

            is UntypedVariableDeclarationAndAssignmentData -> {
                elaborateExpression(stmt.value, scope, table, errors, stmtPos)
                scope.declare(stmt.name, SymbolInfo(
                    type = WaterfallType.fromSourceText(stmt.inferredType),
                    isReadonly = stmt.isImmutable(),
                    kind = SymbolKind.Variable,
                    sourcePosition = stmtPos
                ))
            }

            is VariableAssignmentData ->
                elaborateExpression(stmt.value, scope, table, errors, stmtPos)

            is IncrementStatementData -> { /* no expression sub-tree */ }

            is IfBlockData -> {
                elaborateExpression(stmt.ifBranch.condition, scope, table, errors, stmtPos)
                val thenScope = scope.enterScope()
                stmt.ifBranch.body.forEach { elaborateStatement(it, thenScope, table, errors) }
                scope.exitScope(thenScope)
                for (elif in stmt.elifBranches) {
                    elaborateExpression(elif.condition, scope, table, errors, stmtPos)
                    val elifScope = scope.enterScope()
                    elif.body.forEach { elaborateStatement(it, elifScope, table, errors) }
                    scope.exitScope(elifScope)
                }
                if (stmt.elseBody != null) {
                    val elseScope = scope.enterScope()
                    stmt.elseBody.forEach { elaborateStatement(it, elseScope, table, errors) }
                    scope.exitScope(elseScope)
                }
            }

            is WhileBlockData -> {
                elaborateExpression(stmt.condition, scope, table, errors, stmtPos)
                val bodyScope = scope.enterScope()
                stmt.body.forEach { elaborateStatement(it, bodyScope, table, errors) }
                scope.exitScope(bodyScope)
            }

            is ForBlockData -> {
                val bodyScope = scope.enterScope()
                // Declare iterator so body expressions can resolve it
                bodyScope.declare(stmt.iteratorName, SymbolInfo(
                    type = WaterfallType.IntType,
                    isReadonly = false,
                    kind = SymbolKind.Argument,
                    sourcePosition = stmtPos
                ))
                stmt.body.forEach { elaborateStatement(it, bodyScope, table, errors) }
                scope.exitScope(bodyScope)
            }

            is ReturnStatementData ->
                stmt.value?.let { elaborateExpression(it, scope, table, errors, stmtPos) }

            is FunctionCallStatementData ->
                elaborateFunctionCall(stmt.call, scope, table, errors, stmtPos)

            is FunctionImplementationData -> error(
                "FunctionImplementationData unreachable in Elaboration.elaborateStatement; " +
                "handled at module level by ModuleVerifier"
            )
        }
    }

    /**
     * Elaborate a single [ExpressionData] node and all its sub-expressions.
     * Writes the resolved [WaterfallType] for this node into [table].
     *
     * **OQ-11.3=(a) §4.1**: emits [UnknownIdentifier] into [errors] for unresolved
     * IDENTIFIER / ARRAY_INDEX target / FUNCTION_CALL.LOCAL in expression context.
     * VoidType is still written to [table] for unresolved names to keep the table
     * invariant (every elaborated node has an entry). The IrLowering Void assertion
     * is the drift catch if IrLowering is somehow called on a rejected module.
     *
     * @param primaryPos the enclosing statement's source position (§4.1 fallback;
     *   replaced by [ExpressionData.sourcePosition] in §4.2 once that field exists).
     */
    fun elaborateExpression(
        expr: ExpressionData,
        scope: SymbolTable,
        table: MutableMap<ExpressionData, WaterfallType>,
        errors: MutableList<VerifyError>,
        primaryPos: SourcePosition
    ) {
        val resolvedType: WaterfallType = when (expr.kind) {

            // Scope-dependent — need symbol table
            ExpressionData.Kind.IDENTIFIER -> {
                val name = expr.literalText ?: return
                val resolved = scope.lookup(name)?.type
                if (resolved == null) {
                    // OQ-11.3=(a): emit structured error; keep VoidType in table for IrLowering drift assertion
                    errors += UnknownIdentifier(name, UnknownIdentifier.Context.EXPRESSION, primaryPos)
                }
                resolved ?: WaterfallType.VoidType
            }

            ExpressionData.Kind.FUNCTION_CALL -> {
                val fc = expr.functionCall ?: return
                elaborateFunctionCall(fc, scope, table, errors, primaryPos)
                if (fc.kind == FunctionCallData.Kind.LOCAL) {
                    val resolved = scope.lookup(fc.functionName)?.type
                    if (resolved == null) {
                        // OQ-11.3=(a): emit for expression-context LOCAL call (distinct from statement-level check)
                        errors += UnknownIdentifier(fc.functionName, UnknownIdentifier.Context.EXPRESSION, primaryPos)
                    }
                    resolved ?: WaterfallType.VoidType
                } else {
                    WaterfallType.VoidType  // MODULE/OBJECT: Void placeholder (R3)
                }
            }

            ExpressionData.Kind.ARRAY_INDEX -> {
                val ai = expr.arrayIndex ?: return
                elaborateExpression(ai.index, scope, table, errors, primaryPos)
                val arrayType = scope.lookup(ai.target)?.type
                if (arrayType == null) {
                    // OQ-11.3=(a): emit for unresolved array target
                    errors += UnknownIdentifier(ai.target, UnknownIdentifier.Context.ARRAY_INDEX_TARGET, primaryPos)
                }
                if (arrayType is WaterfallType.ArrayType) arrayType.element
                else WaterfallType.VoidType  // target not in scope or not array → Void placeholder
            }

            // Constant-type — no scope lookup
            ExpressionData.Kind.NULL_LITERAL     -> WaterfallType.VoidType
            ExpressionData.Kind.BOOL_LITERAL     -> WaterfallType.BoolType
            ExpressionData.Kind.INT_LITERAL      -> WaterfallType.IntType
            ExpressionData.Kind.DEC_LITERAL      -> WaterfallType.DecType
            ExpressionData.Kind.STRING_LITERAL   -> WaterfallType.CharType

            ExpressionData.Kind.CAST -> {
                expr.castOperand?.let { elaborateExpression(it, scope, table, errors, primaryPos) }
                val targetText = expr.castTargetType ?: return
                WaterfallType.fromSourceText(targetText)
            }

            ExpressionData.Kind.BINARY_OP -> {
                val l = expr.left ?: return
                val r = expr.right ?: return
                elaborateExpression(l, scope, table, errors, primaryPos)
                elaborateExpression(r, scope, table, errors, primaryPos)
                // R4: no side-table entry for BinaryOp — IrLowering derives the type as
                // `lIr.type` (left operand type, P10 placeholder). P11 §4.3 adds the entry.
                return
            }

            ExpressionData.Kind.ARRAY -> {
                val elements = expr.array?.elements ?: emptyList()
                elements.forEach { elaborateExpression(it, scope, table, errors, primaryPos) }
                if (elements.isEmpty()) {
                    WaterfallType.VoidType  // Q3: empty array → Void placeholder
                } else {
                    table[elements[0]] ?: WaterfallType.VoidType
                }
            }

            ExpressionData.Kind.BUNDLE -> {
                val elements = expr.bundle?.elements ?: emptyList()
                elements.forEach { elaborateExpression(it, scope, table, errors, primaryPos) }
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
                lam.body?.let { body ->
                    elaborateFunctionCall(body, lambdaScope, table, errors, primaryPos)
                    // OQ-11.3=(a): check function name for LOCAL calls in lambda body.
                    // This path isn't covered by elaborateExpression(FUNCTION_CALL) since the
                    // lambda body is a FunctionCallData, not an ExpressionData.
                    if (body.kind == FunctionCallData.Kind.LOCAL &&
                        lambdaScope.lookup(body.functionName) == null) {
                        errors += UnknownIdentifier(
                            body.functionName,
                            UnknownIdentifier.Context.EXPRESSION,
                            primaryPos
                        )
                    }
                }
                scope.exitScope(lambdaScope)
                WaterfallType.VoidType  // Lambda type = Void placeholder
            }
        }
        table[expr] = resolvedType
    }

    private fun elaborateFunctionCall(
        fc: FunctionCallData,
        scope: SymbolTable,
        table: MutableMap<ExpressionData, WaterfallType>,
        errors: MutableList<VerifyError>,
        primaryPos: SourcePosition
    ) {
        fc.positionalArguments.forEach { elaborateExpression(it, scope, table, errors, primaryPos) }
        // namedArguments is List<com.aaroncoplan.waterfall.parser.Pair<String, ExpressionData>>
        // — use .secondVal (not destructuring; the custom Pair has no component functions)
        fc.namedArguments.forEach { elaborateExpression(it.secondVal, scope, table, errors, primaryPos) }
    }
}
