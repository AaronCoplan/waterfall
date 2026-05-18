package com.aaroncoplan.waterfall.compiler.verifier

/**
 * JSON-first renderer stub. Deferred to §5.3.5 or P13.
 *
 * Per §4.8, the full JSON output produces JSONL on stderr (one object per line)
 * with `schemaVersion`, `severity`, `code`, `message`, `primaryLocation`, and
 * optional `relatedInfo`/`suggestedFix`/`tags`. The schema lives at
 * `notes/error-schema-v1.json`. P13's LSP server will consume this stream.
 */
object JsonRenderer : ErrorRenderer {
    override fun render(error: VerifyError): String =
        error("JsonRenderer not implemented; deferred to §5.3.5 or P13 — use HumanRenderer for now")
}
