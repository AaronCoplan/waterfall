package com.aaroncoplan.waterfall.compiler.statements.helpers

import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

interface Translatable {

    fun verify(symbolTable: SymbolTable): VerificationResult

    /**
     * Render this node via the given backend. Implementations should dispatch to the
     * matching `emit*` method on [CodeGenerator] (e.g. `g.emitIfBlock(this)`).
     */
    fun translate(backend: CodeGenerator): String
}
