package com.aaroncoplan.waterfall.compiler.statements

/**
 * Decodes Waterfall STRING_LITERAL source text (backtick-delimited) into a raw
 * String, and re-escapes raw Strings into a target language's quoted form.
 *
 * Source escapes recognized:
 *   `\``   backtick
 *   `\\`   backslash
 *   `\n`   newline
 *   `\r`   carriage return
 *   `\t`   tab
 *
 * Unknown escape sequences pass through with the backslash preserved (defensive).
 */
object StringLiteralText {

    /** Strips the surrounding backticks and resolves source escapes. */
    @JvmStatic
    fun unescape(backtickedSource: String?): String? {
        if (backtickedSource == null) return null
        var body = backtickedSource
        if (body.length >= 2 && body[0] == '`' && body[body.length - 1] == '`') {
            body = body.substring(1, body.length - 1)
        }
        val out = StringBuilder(body.length)
        var i = 0
        while (i < body.length) {
            val c = body[i]
            if (c != '\\' || i == body.length - 1) {
                out.append(c)
                i++
                continue
            }
            when (body[i + 1]) {
                '`'  -> { out.append('`');  i += 2 }
                '\\' -> { out.append('\\'); i += 2 }
                'n'  -> { out.append('\n'); i += 2 }
                'r'  -> { out.append('\r'); i += 2 }
                't'  -> { out.append('\t'); i += 2 }
                else -> {
                    // Unknown escape — preserve the backslash defensively.
                    out.append('\\')
                    i++
                }
            }
        }
        return out.toString()
    }

    /**
     * Re-encodes a raw string for a target language. The [quote] character is what
     * surrounds the result and which character to escape inside; backslash and
     * control chars are always escaped.
     */
    @JvmStatic
    fun escapeFor(raw: String?, quote: Char): String? {
        if (raw == null) return null
        val out = StringBuilder(raw.length + 2)
        out.append(quote)
        for (c in raw) {
            when (c) {
                '\\'  -> out.append("\\\\")
                quote -> { out.append('\\'); out.append(quote) }
                '\n'  -> out.append("\\n")
                '\r'  -> out.append("\\r")
                '\t'  -> out.append("\\t")
                else  -> out.append(c)
            }
        }
        out.append(quote)
        return out.toString()
    }
}
