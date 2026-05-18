package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.statements.*
import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Single pass converting a verified [ModuleAst] to an [IrModule] consumable
 * by backends (after §5.5 migrates them from `*Data`).
 *
 * ## Contract (F1=C)
 *
 * - Input: [ModuleAst] + `resolvedTypes` side-table from [com.aaroncoplan.waterfall.compiler.verifier.VerifyResult].
 * - The symbol table is NOT passed — type info comes from [resolvedTypes].
 * - Throws [IllegalStateException] if [resolvedTypes] is missing an entry for a
 *   scope-dependent expression. Message: `"$name undeclared at $pos; verifier should
 *   have caught this"`. In practice, [com.aaroncoplan.waterfall.compiler.verifier.Elaboration]
 *   stores `WaterfallType.VoidType` for undeclared identifiers (per OQ-3=C), so undeclared
 *   names lower to `IrExpression.Identifier("x", IrType.Void)` rather than throwing.
 *
 * §5.4 scope: `Main.kt` does NOT invoke this; [IrLoweringTest] is the only consumer.
 * Backends migrate to IR in §5.5.
 */
object IrLowering {

    fun lowerModule(module: ModuleAst, resolvedTypes: Map<ExpressionData, WaterfallType>): IrModule {
        // Derive a module-level source position from the first available item.
        val modulePos: SourcePosition = module.functions.firstOrNull()?.getSourcePosition()
            ?: module.topLevelVariables.firstOrNull()?.getSourcePosition()
            ?: SourcePosition("(module)", 1, 0)

        val topLevelVars = module.topLevelVariables.map { v ->
            IrTopLevelVariable(
                name = v.name,
                type = IrType.fromWaterfallType(WaterfallType.fromSourceText(v.type)),
                isReadonly = v.isImmutable(),   // R1: isImmutable() not modifiers.contains("readonly")
                initializer = lowerExpression(v.value, resolvedTypes, v.getSourcePosition()),
                sourcePosition = v.getSourcePosition()
            )
        }

        val functions = module.functions.map { f ->
            // typedArguments: List<parser.Pair<String,String>> — firstVal=type, secondVal=name (legacy ordering)
            val params = f.typedArguments.map { arg ->
                IrParameter(
                    name = arg.secondVal,
                    type = IrType.fromWaterfallType(WaterfallType.fromSourceText(arg.firstVal)),
                    sourcePosition = f.getSourcePosition()  // TODO(P10): per-arg positions (PITFALL #8)
                )
            }
            IrFunction(
                name = f.name,
                parameters = params,
                returnType = IrType.fromWaterfallType(WaterfallType.forReturnType(f.returnType)),
                body = f.statements.map { lowerStatement(it, resolvedTypes) },
                sourcePosition = f.getSourcePosition()
            )
        }

        return IrModule(
            name = module.name,
            topLevelVariables = topLevelVars,
            functions = functions,
            sourcePosition = modulePos
        )
    }

    // ---------------------------------------------------------------------- //
    // Statement lowering
    // ---------------------------------------------------------------------- //

    private fun lowerStatement(
        stmt: TranslatableStatement,
        resolvedTypes: Map<ExpressionData, WaterfallType>
    ): IrStatement {
        val pos = stmt.getSourcePosition()
        return when (stmt) {
            is TypedVariableDeclarationAndAssignmentData -> IrStatement.TypedVarDecl(
                name = stmt.name,
                type = IrType.fromWaterfallType(WaterfallType.fromSourceText(stmt.type)),
                isReadonly = stmt.isImmutable(),
                initializer = lowerExpression(stmt.value, resolvedTypes, pos),
                sourcePosition = pos
            )
            is UntypedVariableDeclarationAndAssignmentData -> IrStatement.UntypedVarDecl(
                name = stmt.name,
                inferredType = IrType.fromWaterfallType(WaterfallType.fromSourceText(stmt.inferredType)),
                isReadonly = stmt.isImmutable(),
                initializer = lowerExpression(stmt.value, resolvedTypes, pos),
                sourcePosition = pos
            )
            is VariableAssignmentData -> IrStatement.VarAssignment(
                name = stmt.name,
                op = stmt.op,
                value = lowerExpression(stmt.value, resolvedTypes, pos),
                sourcePosition = pos
            )
            is IncrementStatementData -> IrStatement.IncrementStatement(
                name = stmt.name,
                op = stmt.op,
                sourcePosition = pos
            )
            is IfBlockData -> IrStatement.IfBlock(
                ifBranch = IrStatement.IfBlock.Branch(
                    condition = lowerExpression(stmt.ifBranch.condition, resolvedTypes, pos),
                    body = stmt.ifBranch.body.map { lowerStatement(it, resolvedTypes) }
                ),
                elifBranches = stmt.elifBranches.map { elif ->
                    IrStatement.IfBlock.Branch(
                        condition = lowerExpression(elif.condition, resolvedTypes, pos),
                        body = elif.body.map { lowerStatement(it, resolvedTypes) }
                    )
                },
                elseBody = stmt.elseBody?.map { lowerStatement(it, resolvedTypes) },
                sourcePosition = pos
            )
            is WhileBlockData -> IrStatement.WhileBlock(
                condition = lowerExpression(stmt.condition, resolvedTypes, pos),
                body = stmt.body.map { lowerStatement(it, resolvedTypes) },
                sourcePosition = pos
            )
            is ForBlockData -> IrStatement.ForBlock(
                iteratorName = stmt.iteratorName,
                collectionName = stmt.collectionName,
                body = stmt.body.map { lowerStatement(it, resolvedTypes) },
                sourcePosition = pos
            )
            is ReturnStatementData -> IrStatement.ReturnStatement(
                value = stmt.value?.let { lowerExpression(it, resolvedTypes, pos) },
                sourcePosition = pos
            )
            is FunctionCallStatementData -> IrStatement.FunctionCallStatement(
                call = lowerFunctionCall(stmt.call, resolvedTypes, pos),
                sourcePosition = pos
            )
            else -> error("IrLowering: unexpected statement kind ${stmt::class.simpleName} at ${pos.generateMessage()}")
        }
    }

    // ---------------------------------------------------------------------- //
    // Expression lowering
    // ---------------------------------------------------------------------- //

    private fun lowerExpression(
        expr: ExpressionData,
        resolvedTypes: Map<ExpressionData, WaterfallType>,
        fallbackPos: SourcePosition
    ): IrExpression {
        return when (expr.kind) {
            ExpressionData.Kind.NULL_LITERAL -> IrExpression.NullLiteral(
                type = IrType.Void, sourcePosition = fallbackPos
            )
            ExpressionData.Kind.BOOL_LITERAL -> IrExpression.BoolLiteral(
                value = expr.literalText == "true", sourcePosition = fallbackPos
            )
            ExpressionData.Kind.INT_LITERAL -> IrExpression.IntLiteral(
                literalText = expr.literalText ?: "0", sourcePosition = fallbackPos
            )
            ExpressionData.Kind.DEC_LITERAL -> IrExpression.DecLiteral(
                literalText = expr.literalText ?: "0.0", sourcePosition = fallbackPos
            )
            ExpressionData.Kind.STRING_LITERAL -> IrExpression.StringLiteral(
                literalText = expr.literalText ?: "", sourcePosition = fallbackPos
            )
            ExpressionData.Kind.IDENTIFIER -> {
                val name = expr.literalText
                    ?: throw IllegalStateException("IDENTIFIER node has null literalText at ${fallbackPos.generateMessage()}")
                val waterfallType = resolvedTypes[expr]
                    ?: throw IllegalStateException(
                        "$name undeclared at ${fallbackPos.generateMessage()}; verifier should have caught this"
                    )
                IrExpression.Identifier(name, IrType.fromWaterfallType(waterfallType), fallbackPos)
            }
            ExpressionData.Kind.FUNCTION_CALL -> {
                val fc = expr.functionCall
                    ?: throw IllegalStateException("FUNCTION_CALL node has null functionCall at ${fallbackPos.generateMessage()}")
                val returnType = resolvedTypes[expr]?.let { IrType.fromWaterfallType(it) } ?: IrType.Void
                lowerFunctionCall(fc, resolvedTypes, fallbackPos).copy(type = returnType)
            }
            ExpressionData.Kind.BINARY_OP -> {
                val l = expr.left ?: throw IllegalStateException("BINARY_OP missing left at ${fallbackPos.generateMessage()}")
                val r = expr.right ?: throw IllegalStateException("BINARY_OP missing right at ${fallbackPos.generateMessage()}")
                val lIr = lowerExpression(l, resolvedTypes, fallbackPos)
                val rIr = lowerExpression(r, resolvedTypes, fallbackPos)
                IrExpression.BinaryOp(
                    op = expr.op ?: "?",
                    left = lIr,
                    right = rIr,
                    type = lIr.type,   // P10: left.type placeholder
                    sourcePosition = fallbackPos
                )
            }
            ExpressionData.Kind.CAST -> {
                val operand = expr.castOperand
                    ?: throw IllegalStateException("CAST missing operand at ${fallbackPos.generateMessage()}")
                val targetWf = WaterfallType.fromSourceText(expr.castTargetType ?: "void")
                IrExpression.Cast(
                    targetType = IrType.fromWaterfallType(targetWf),
                    operand = lowerExpression(operand, resolvedTypes, fallbackPos),
                    sourcePosition = fallbackPos
                )
            }
            ExpressionData.Kind.ARRAY_INDEX -> {
                val ai = expr.arrayIndex
                    ?: throw IllegalStateException("ARRAY_INDEX node has null arrayIndex at ${fallbackPos.generateMessage()}")
                val elementType = resolvedTypes[expr]?.let { IrType.fromWaterfallType(it) } ?: IrType.Void
                IrExpression.ArrayIndex(
                    target = ai.target,
                    index = lowerExpression(ai.index, resolvedTypes, fallbackPos),
                    type = elementType,
                    sourcePosition = fallbackPos
                )
            }
            ExpressionData.Kind.ARRAY -> {
                val elements = expr.array?.elements ?: emptyList()
                val irElements = elements.map { lowerExpression(it, resolvedTypes, fallbackPos) }
                val arrayType = if (irElements.isEmpty()) {
                    IrType.Void  // Q3: empty array → Void placeholder
                } else {
                    IrType.Array(irElements[0].type)
                }
                IrExpression.ArrayLiteral(irElements, arrayType, fallbackPos)
            }
            ExpressionData.Kind.BUNDLE -> {
                val elements = expr.bundle?.elements ?: emptyList()
                IrExpression.BundleLiteral(
                    elements = elements.map { lowerExpression(it, resolvedTypes, fallbackPos) },
                    type = IrType.Void,
                    sourcePosition = fallbackPos
                )
            }
            ExpressionData.Kind.LAMBDA -> {
                val lam = expr.lambda
                    ?: throw IllegalStateException("LAMBDA node has null lambda at ${fallbackPos.generateMessage()}")
                // firstVal=type, secondVal=name (legacy ordering; swap per §1.3)
                val params = lam.typedArguments.map { arg ->
                    IrParameter(
                        name = arg.secondVal,
                        type = IrType.fromWaterfallType(WaterfallType.fromSourceText(arg.firstVal)),
                        sourcePosition = fallbackPos
                    )
                }
                val body = lam.body?.let { lowerFunctionCall(it, resolvedTypes, fallbackPos) }
                IrExpression.Lambda(params, body, IrType.Void, fallbackPos)
            }
        }
    }

    // ---------------------------------------------------------------------- //
    // Function-call lowering (shared between expression + statement)
    // ---------------------------------------------------------------------- //

    private fun lowerFunctionCall(
        fc: FunctionCallData,
        resolvedTypes: Map<ExpressionData, WaterfallType>,
        pos: SourcePosition
    ): IrExpression.FunctionCall {
        val kind = when (fc.kind) {
            FunctionCallData.Kind.LOCAL  -> IrExpression.FunctionCall.Kind.Local
            FunctionCallData.Kind.MODULE -> IrExpression.FunctionCall.Kind.Module
            FunctionCallData.Kind.OBJECT -> IrExpression.FunctionCall.Kind.Object
        }
        return IrExpression.FunctionCall(
            kind = kind,
            moduleName = fc.moduleName,
            receiverPath = fc.receiverPath,
            functionName = fc.functionName,
            positionalArguments = fc.positionalArguments.map { lowerExpression(it, resolvedTypes, pos) },
            namedArguments = fc.namedArguments.map { pair ->
                kotlin.Pair(pair.firstVal, lowerExpression(pair.secondVal, resolvedTypes, pos))
            },
            type = IrType.Void,  // caller overrides for expression context (FUNCTION_CALL kind)
            sourcePosition = pos
        )
    }
}
