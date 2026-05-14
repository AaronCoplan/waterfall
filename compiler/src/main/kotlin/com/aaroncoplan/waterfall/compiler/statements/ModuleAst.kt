package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser

/**
 * Aggregates a module's top-level declarations after parsing. Each backend is given
 * one of these and decides how to render the module as a whole (e.g. namespace
 * wrapping, header comments, output ordering).
 */
class ModuleAst(filePath: String, ctx: WaterfallParser.ModuleContext) {

    @JvmField val name: String = ctx.name.text

    @JvmField val topLevelVariables: List<TypedVariableDeclarationAndAssignmentData>
    @JvmField val functions: List<FunctionImplementationData>

    init {
        val vars = mutableListOf<TypedVariableDeclarationAndAssignmentData>()
        val fns = mutableListOf<FunctionImplementationData>()
        for (tld in ctx.topLevelDeclaration()) {
            when {
                tld.typedVariableDeclarationAndAssignment() != null ->
                    vars.add(TypedVariableDeclarationAndAssignmentData(filePath, tld.typedVariableDeclarationAndAssignment()))
                tld.functionImplementation() != null ->
                    fns.add(FunctionImplementationData(filePath, tld.functionImplementation()))
            }
        }
        topLevelVariables = vars.toList()
        functions = fns.toList()
    }
}
