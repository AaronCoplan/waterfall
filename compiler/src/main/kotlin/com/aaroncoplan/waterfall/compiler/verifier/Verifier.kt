package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable

/**
 * Top-level entry point for module verification. Takes a [ModuleAst] and a
 * fresh [SymbolTable]; returns a [VerifyResult] carrying any errors found.
 *
 * The symbol table is mutated as declarations land. The caller (Main.run)
 * passes the same table to §5.4's IrLowering once verification succeeds.
 *
 * OQ-2 (B=drop): no `target` parameter in P10. The target-conditional check
 * (`@external` partial-support) is deferred to P12. When P12 lands, the
 * signature will gain `target: TargetKeyword? = null`.
 */
object Verifier {
    fun verifyModule(module: ModuleAst, symbolTable: SymbolTable): VerifyResult {
        return ModuleVerifier.verifyModule(module, symbolTable)
    }
}
