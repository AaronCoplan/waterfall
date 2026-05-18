package com.aaroncoplan.waterfall.compiler.statements.helpers

import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

interface Translatable {

    /**
     * Render this node via the given backend. Implementations should dispatch to the
     * matching `emit*` method on [CodeGenerator] (e.g. `g.emitIfBlock(this)`).
     *
     * Note: `verify(symbolTable)` has been removed from this interface in §5.3.
     * Verification now lives in `compiler/.../verifier/` — see `Verifier.verifyModule`.
     */
    fun translate(backend: CodeGenerator): String
}
