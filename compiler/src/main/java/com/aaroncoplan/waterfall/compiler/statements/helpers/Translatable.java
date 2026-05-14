package com.aaroncoplan.waterfall.compiler.statements.helpers;

import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator;

public interface Translatable {
    VerificationResult verify(SymbolTable symbolTable);

    /**
     * Render this node via the given backend. Implementations should dispatch to the
     * matching emit* method on {@link CodeGenerator} (e.g. {@code g.emitIfBlock(this)}).
     */
    String translate(CodeGenerator backend);
}
