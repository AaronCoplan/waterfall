package com.aaroncoplan.waterfall.compiler.typesystem

/**
 * A Waterfall type expression. Closed sum type — every variant must be matched
 * exhaustively. Adding a variant requires updating every `when (t)` site.
 *
 * Equality is structural (data classes / data objects). Pretty-printing via
 * [render] is the canonical user-facing form.
 */
sealed class WaterfallType {

    /** Render as the source-language form. Inverse of parsing a `type` rule. */
    abstract fun render(): String

    /** The four scalar primitives. */
    data object IntType : WaterfallType() { override fun render() = "int" }
    data object DecType : WaterfallType() { override fun render() = "dec" }
    data object BoolType : WaterfallType() { override fun render() = "bool" }
    data object CharType : WaterfallType() { override fun render() = "char" }

    /** Array of a primitive (today only; later phases may relax). */
    data class ArrayType(val element: WaterfallType) : WaterfallType() {
        override fun render() = "${element.render()}[]"
    }

    /**
     * The return type for a function with no `returns` clause. Distinct from
     * any value type. `VoidType.render()` returns `"void"` by convention.
     */
    data object VoidType : WaterfallType() { override fun render() = "void" }

    /**
     * A type we couldn't resolve. Carries the source text the parser saw so
     * the verifier can produce a friendly error. ErrorType never appears in
     * valid programs that pass verification.
     */
    data class ErrorType(val sourceText: String) : WaterfallType() {
        override fun render() = "<error:$sourceText>"
    }

    companion object {
        /**
         * Parse a `type` rule's text (e.g. "int", "int[]", "?int") into a
         * WaterfallType. Returns ErrorType when the text isn't recognized.
         *
         * For P10 we accept exactly what today's `PrimitiveTypes.isPrimitiveOrArray`
         * accepts. The leading `?` (nullable) is parsed but produces ErrorType —
         * matches today's behavior (audit surprise #5).
         *
         * **Whitespace policy:** this function is strict-literal. It does NOT trim,
         * normalize, or lowercase its input. `"int "`, `" int"`, `"int []"`, and
         * `"INT"` all return `ErrorType(text)`. The caller is responsible for passing
         * exactly the text that appeared in the source.
         *
         * **ErrorType.sourceText preservation:** every code path that returns
         * `ErrorType` passes the original `text` argument byte-identical as the
         * `sourceText` field. Callers that need to recover the raw input can always
         * retrieve it from `ErrorType.sourceText`.
         *
         * **Totality:** this function never throws. Every possible input produces a
         * valid `WaterfallType` instance. `ErrorType` is the only failure surface.
         */
        fun fromSourceText(text: String): WaterfallType {
            if (text.startsWith("?")) return ErrorType(text)
            val isArray = text.endsWith("[]")
            val base = if (isArray) text.dropLast(2) else text
            val baseType = when (base) {
                "int"  -> IntType
                "dec"  -> DecType
                "bool" -> BoolType
                "char" -> CharType
                "void" -> VoidType
                else   -> return ErrorType(text)
            }
            // `void[]` is never a valid type — `void` is a return-only marker,
            // not a value type that can be put in an array. The verifier-level
            // PITFALL #1 covers bare `void x = ...`; this guard covers the array
            // case so `ArrayType(VoidType)` is structurally unconstructible.
            if (isArray && baseType === VoidType) return ErrorType(text)
            return if (isArray) ArrayType(baseType) else baseType
        }

        /**
         * Canonical adapter for nullable inputs (e.g., `FunctionImplementationData.returnType: String?`).
         * Returns [VoidType] when `text` is null — matching today's `returnType ?: "void"` semantics
         * but without round-tripping through [fromSourceText] ("void"). Non-null inputs go through
         * [fromSourceText] verbatim.
         *
         * The §5.2 callsites at `FunctionImplementationData.verify` (line 35, self-decl for recursion)
         * MUST use this method, not `fromSourceText(returnType ?: "void")` — the latter is forbidden
         * because it introduces a string-bridge ambiguity (today the string-bridged path agrees with
         * the helper only by coincidence with the §1.2 guard on `void[]`).
         */
        fun forReturnType(text: String?): WaterfallType =
            if (text == null) VoidType else fromSourceText(text)
    }
}
