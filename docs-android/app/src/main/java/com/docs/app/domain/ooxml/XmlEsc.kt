package com.docs.app.domain.ooxml

/**
 * XML escaping and sanitizing utilities for generating Office Open XML parts.
 *
 * XML 1.0 allows the full range up to U+FFFD except the XML-1.0 control
 * characters and the (unpaired) surrogate code points. Kotlin strings are
 * UTF-16, so astral characters arrive as surrogate pairs — well-formed pairs
 * pass through untouched, lone surrogates are dropped.
 */
object XmlEsc {

    /** Escapes the five predefined XML entities. */
    fun esc(raw: String): String {
        val sb = StringBuilder(raw.length + 16)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            when {
                c == '&' -> sb.append("&amp;")
                c == '<' -> sb.append("&lt;")
                c == '>' -> sb.append("&gt;")
                c == '"' -> sb.append("&quot;")
                c == '\'' -> sb.append("&apos;")
                isHighSurrogate(c) && i + 1 < raw.length && isLowSurrogate(raw[i + 1]) -> {
                    sb.append(c)
                    sb.append(raw[i + 1])
                    i += 1 // skip the low half; +1 more below
                }
                isSurrogate(c) -> Unit // lone surrogate — drop
                isLegalXmlChar(c) -> sb.append(c)
            }
            i += 1
        }
        return sb.toString()
    }

    /** Same as [esc] but safe to embed inside a double-quoted attribute. */
    fun attr(raw: String): String = esc(raw)

    /** Strips characters that are illegal in XML 1.0 documents. */
    fun stripIllegal(raw: String): String {
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            when {
                isHighSurrogate(c) && i + 1 < raw.length && isLowSurrogate(raw[i + 1]) -> {
                    sb.append(c)
                    sb.append(raw[i + 1])
                    i += 1
                }
                isSurrogate(c) -> Unit
                isLegalXmlChar(c) -> sb.append(c)
            }
            i += 1
        }
        return sb.toString()
    }

    fun isLegalXmlChar(c: Char): Boolean {
        if (c == '\t' || c == '\n' || c == '\r') return true
        if (c in '\u0020'..'\uD7FF') return true
        if (c in '\uE000'..'\uFFFD') return true   // private use up to replacement char
        return false
    }

    private fun isSurrogate(c: Char): Boolean = c in '\uD800'..'\uDFFF'
    private fun isHighSurrogate(c: Char): Boolean = c in '\uD800'..'\uDBFF'
    private fun isLowSurrogate(c: Char): Boolean = c in '\uDC00'..'\uDFFF'
}
