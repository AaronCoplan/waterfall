package com.aaroncoplan.waterfall.compiler.verifier

/**
 * Renders a [VerifyError] to a user-facing string. P10 ships two implementations:
 * [HumanRenderer] (simple format, byte-identical strings per §5.2 contract) and
 * [JsonRenderer] (stub, deferred to §5.3.5 or P13).
 *
 * P13 may introduce a `diagnostics/` package with richer renderers (colorized
 * multi-line Elm/Roc style, LSP Diagnostic serializer, etc.). For now, all
 * renderer files live in `verifier/` alongside the error types they render.
 */
interface ErrorRenderer {
    fun render(error: VerifyError): String
}
