package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.parser.Pair

class FunctionCallData(filePath: String, ctx: WaterfallParser.FunctionCallContext) {

    enum class Kind { LOCAL, MODULE, OBJECT }

    @JvmField val kind: Kind
    /** Set only when [kind] is [Kind.MODULE]. */
    @JvmField val moduleName: String?
    /** Set only when [kind] is [Kind.OBJECT] — the dotted path before the function name. */
    @JvmField val receiverPath: List<String>
    @JvmField val functionName: String
    @JvmField val positionalArguments: List<ExpressionData>
    @JvmField val namedArguments: List<Pair<String, ExpressionData>>

    init {
        when {
            ctx.localFunctionCall() != null -> {
                val local = ctx.localFunctionCall()
                kind = Kind.LOCAL
                moduleName = null
                receiverPath = emptyList()
                functionName = local.functionName.text
                val args = extractArgs(filePath, local.functionCallArguments())
                positionalArguments = args.firstVal
                namedArguments = args.secondVal
            }
            ctx.moduleFunctionCall() != null -> {
                val mod = ctx.moduleFunctionCall()
                kind = Kind.MODULE
                moduleName = mod.moduleName.text
                receiverPath = emptyList()
                functionName = mod.functionName.text
                val args = extractArgs(filePath, mod.functionCallArguments())
                positionalArguments = args.firstVal
                namedArguments = args.secondVal
            }
            ctx.objectFunctionCall() != null -> {
                val obj = ctx.objectFunctionCall()
                kind = Kind.OBJECT
                moduleName = null
                // Grammar: name=ID DOT (name=ID DOT)* functionName=ID L_PARENS ...
                // Every ID() except the last is part of the receiver path; the last is functionName.
                functionName = obj.functionName.text
                val ids = obj.ID().map { it.text }.toMutableList()
                if (ids.isNotEmpty() && ids.last() == functionName) {
                    ids.removeAt(ids.size - 1)
                }
                receiverPath = ids.toList()
                val args = extractArgs(filePath, obj.functionCallArguments())
                positionalArguments = args.firstVal
                namedArguments = args.secondVal
            }
            else -> throw RuntimeException("Unrecognized functionCall variant")
        }
    }

    companion object {
        private fun extractArgs(
            filePath: String,
            args: WaterfallParser.FunctionCallArgumentsContext?
        ): Pair<List<ExpressionData>, List<Pair<String, ExpressionData>>> {
            if (args == null) return Pair(emptyList(), emptyList())
            args.namedArguments()?.let { named ->
                val parsed = named.namedArgument().map { n ->
                    Pair(n.name.text, ExpressionData(filePath, n.value))
                }
                return Pair(emptyList(), parsed)
            }
            args.positionalArguments?.let { positional ->
                val parsed = positional.expression().map { ExpressionData(filePath, it) }
                return Pair(parsed, emptyList())
            }
            return Pair(emptyList(), emptyList())
        }
    }
}
