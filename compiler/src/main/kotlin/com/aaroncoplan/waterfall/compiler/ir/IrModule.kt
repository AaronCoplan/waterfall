package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition

/**
 * The IR for a single Waterfall module. Produced by [IrLowering], consumed by
 * each backend's emitProgram() (after §5.5 migrates backends to IR).
 */
data class IrModule(
    val name: String,
    val topLevelVariables: List<IrTopLevelVariable>,
    val functions: List<IrFunction>,
    val sourcePosition: SourcePosition
)

data class IrTopLevelVariable(
    val name: String,
    val type: IrType,
    /** True iff the source declaration used `const` or `imm` (§5.2 isImmutable() preservation). */
    val isReadonly: Boolean,
    val initializer: IrExpression,
    val sourcePosition: SourcePosition
)

data class IrFunction(
    val name: String,
    val parameters: List<IrParameter>,
    val returnType: IrType,         // IrType.Void for void functions
    val body: List<IrStatement>,
    val sourcePosition: SourcePosition
)

data class IrParameter(
    val name: String,
    val type: IrType,
    val sourcePosition: SourcePosition
)
