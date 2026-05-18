package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.statements.ExpressionData
import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Single pass converting a verified [ModuleAst] (with its `*Data` statements) to
 * an [IrModule] consumable by backends (after §5.5 migrates them).
 *
 * ## Contract (F1=C)
 *
 * - Input: a [ModuleAst] + the `resolvedTypes` side-table from [VerifyResult]
 *   (populated by [Elaboration] during the verify walk).
 * - The symbol table is NOT passed to lowering — use [resolvedTypes] for types.
 * - Output: an [IrModule]. Throws [IllegalStateException] if [resolvedTypes] is
 *   missing an entry for a scope-dependent expression (IDENTIFIER/FUNCTION_CALL/
 *   ARRAY_INDEX). Message: `"$name undeclared at $pos; verifier should have caught this"`.
 *
 * **"Escalate" in §3.8 = `throw IllegalStateException(...)`**. Not a return value;
 * not a user-facing error. P10 post-verification should be clean.
 *
 * ## §5.4 scope
 *
 * `Main.kt` does NOT invoke IrLowering in §5.4. Backends still consume `*Data`
 * directly. [IrLoweringTest] is the only §5.4 consumer.
 * Backends migrate to IR in §5.5.
 */
object IrLowering {

    fun lowerModule(module: ModuleAst, resolvedTypes: Map<ExpressionData, WaterfallType>): IrModule {
        // TODO(§5.4 commit 4): implement per §3.8 lowering algorithm
        TODO("IrLowering.lowerModule — implemented in commit 4")
    }
}
