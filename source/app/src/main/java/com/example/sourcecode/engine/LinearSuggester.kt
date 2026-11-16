package com.example.sourcecode.engine

/**
 * CÁCH 1 – Quét toàn bộ danh sách (tương đương câu SQL trong sách):
 *
 *   SELECT * FROM frequency_table
 *   WHERE query LIKE 'prefix%'
 *   ORDER BY frequency DESC LIMIT 5;
 *
 * Mỗi lần gõ phím phải duyệt hết N truy vấn rồi sắp xếp các truy vấn khớp.
 * Độ phức tạp: O(N) + O(m log m), với m là số truy vấn khớp tiền tố.
 */
class LinearSuggester(entries: List<QueryEntry>) : Suggester {
    override val name = "1. Quét danh sách (như SQL LIKE)"
    override val complexity = "O(N) + O(m log m)"

    private val data: List<QueryEntry> = entries.toList()

    override fun suggest(prefix: String, k: Int): List<Suggestion> {
        val p = Normalizer.normalize(prefix)
        if (p.isEmpty()) return emptyList()
        return data.asSequence()
            .filter { it.key.startsWith(p) }
            .map { Suggestion(it.text, it.frequency) }
            .sortedWith(SUGGESTION_ORDER)
            .take(k)
            .toList()
    }

    override fun storageUnits(): Long = data.size.toLong()
}
