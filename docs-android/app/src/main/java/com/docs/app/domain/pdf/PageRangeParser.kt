package com.docs.app.domain.pdf

/**
 * Parses 1-based page specifications like "all", "1,3,5" or "2-4,7"
 * into a sorted, de-duplicated list of 0-based page indices.
 *
 * Pure Kotlin — no Android dependencies.
 */
object PageRangeParser {

    fun parse(spec: String?, totalPages: Int): List<Int> {
        if (totalPages <= 0) return emptyList()
        val trimmed = spec?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.equals("all", ignoreCase = true)) {
            return (0 until totalPages).toList()
        }
        val selected = sortedSetOf<Int>()
        for (rawPart in trimmed.split(',')) {
            val part = rawPart.trim()
            if (part.isEmpty()) continue
            if ('-' in part) {
                val bounds = part.split('-', limit = 2)
                val start = bounds[0].trim().toIntOrNull()
                val endRaw = bounds.getOrNull(1)?.trim()
                val end = endRaw?.toIntOrNull()
                when {
                    start != null && end != null ->
                        addRange(selected, start, end, totalPages)
                    // Open-ended range, e.g. "3-" means "page 3 to the end"
                    start != null && endRaw.isNullOrEmpty() ->
                        addRange(selected, start, totalPages, totalPages)
                    // Leading range, e.g. "-4" means "pages 1 to 4"
                    start == null && end != null ->
                        addRange(selected, 1, end, totalPages)
                }
            } else {
                part.toIntOrNull()?.let { page ->
                    if (page in 1..totalPages) selected += page - 1
                }
            }
        }
        return selected.toList()
    }

    /** Number of pages the spec resolves to (0 when invalid). */
    fun count(spec: String?, totalPages: Int): Int = parse(spec, totalPages).size

    private fun addRange(target: java.util.SortedSet<Int>, from: Int, to: Int, totalPages: Int) {
        val lo = minOf(from, to)
        val hi = maxOf(from, to)
        for (page in lo..hi) {
            if (page in 1..totalPages) target += page - 1
        }
    }
}
