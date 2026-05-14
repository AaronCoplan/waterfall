package com.aaroncoplan.waterfall.compiler.statements;

/**
 * Decodes Waterfall STRING_LITERAL source text (backtick-delimited) into a raw
 * String, and re-escapes raw Strings into a target language's quoted form.
 *
 * Source escapes recognized:
 *   \`   backtick
 *   \\   backslash
 *   \n   newline
 *   \r   carriage return
 *   \t   tab
 *
 * Unknown escape sequences pass through with the backslash preserved (defensive).
 */
public final class StringLiteralText {

    private StringLiteralText() {}

    /** Strips the surrounding backticks and resolves source escapes. */
    public static String unescape(String backtickedSource) {
        if (backtickedSource == null) return null;
        // Strip the outer backticks if present.
        String body = backtickedSource;
        if (body.length() >= 2
                && body.charAt(0) == '`'
                && body.charAt(body.length() - 1) == '`') {
            body = body.substring(1, body.length() - 1);
        }
        StringBuilder out = new StringBuilder(body.length());
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c != '\\' || i == body.length() - 1) {
                out.append(c);
                continue;
            }
            char next = body.charAt(i + 1);
            switch (next) {
                case '`':  out.append('`'); i++; break;
                case '\\': out.append('\\'); i++; break;
                case 'n':  out.append('\n'); i++; break;
                case 'r':  out.append('\r'); i++; break;
                case 't':  out.append('\t'); i++; break;
                default:
                    // Unknown escape — preserve both chars defensively.
                    out.append('\\');
                    break;
            }
        }
        return out.toString();
    }

    /**
     * Re-encodes a raw string for a target language. The {@code quote} character
     * is what surrounds the result and which character to escape inside; backslash
     * and control chars are always escaped.
     */
    public static String escapeFor(String raw, char quote) {
        if (raw == null) return null;
        StringBuilder out = new StringBuilder(raw.length() + 2);
        out.append(quote);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\')      out.append("\\\\");
            else if (c == quote) out.append('\\').append(quote);
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else                 out.append(c);
        }
        out.append(quote);
        return out.toString();
    }
}
