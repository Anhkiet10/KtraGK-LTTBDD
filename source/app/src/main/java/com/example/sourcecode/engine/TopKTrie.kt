package com.example.sourcecode.engine

/**
 * CÁCH 3 – Cây Trie đã tối ưu theo sách (slide 5 / mục 4.6–4.8 báo cáo):
 *
 *  Tối ưu 1: giới hạn độ dài tiền tố (MAX_PREFIX = 50)  -> tìm nút O(1)
 *  Tối ưu 2: lưu sẵn top-k truy vấn tại MỖI nút          -> lấy kết quả O(1)
 *
 * Đánh đổi: tốn thêm bộ nhớ (mỗi nút giữ tối đa k gợi ý) và khi cập nhật một truy vấn
 * phải sửa lại danh sách top-k của tất cả nút tổ tiên (ví dụ "beer" 10 -> 30 ở Chương 7).
 */
class TopKTrie(entries: List<QueryEntry> = emptyList(), private val k: Int = 5) : Suggester {
    override val name = "3. Trie lưu sẵn top-k"
    override val complexity = "O(1) + O(1)"

    companion object {
        const val MAX_PREFIX = 50
    }

    private class Node {
        val children = HashMap<Char, Node>()
        val topK = ArrayList<Suggestion>(0)
    }

    private val root = Node()
    private var nodeCount = 1L
    private var cachedItems = 0L

    init {
        // Mẹo khi XÂY cây (Workers xây lại cây mỗi tuần): chèn theo tần suất giảm dần,
        // khi đó k truy vấn đầu tiên đi qua một nút chính là top-k của nút đó -> không cần sắp xếp.
        entries.sortedWith(compareByDescending<QueryEntry> { it.frequency }.thenBy { it.text })
            .forEach { buildInsert(it) }
    }

    private fun buildInsert(entry: QueryEntry) {
        val s = Suggestion(entry.text, entry.frequency)
        var node = root
        for ((depth, ch) in entry.key.withIndex()) {
            node = node.children.getOrPut(ch) { nodeCount++; Node() }
            if (depth < MAX_PREFIX && node.topK.size < k) {
                node.topK.add(s); cachedItems++
            }
        }
    }

    /**
     * CẬP NHẬT một truy vấn (cách 2 ở mục 7.2 báo cáo – sửa trực tiếp từng nút).
     * Mọi nút trên đường đi từ gốc đến truy vấn đều phải cập nhật lại top-k.
     * Dùng cho trường hợp tần suất chỉ TĂNG (đếm số lần tìm kiếm).
     * Trả về số nút đã phải sửa để minh họa chi phí cập nhật.
     */
    fun upsert(text: String, frequency: Long): Int {
        val key = Normalizer.normalize(text)
        val s = Suggestion(text, frequency)
        var node = root
        var touched = 0
        for ((depth, ch) in key.withIndex()) {
            node = node.children.getOrPut(ch) { nodeCount++; Node() }
            if (depth >= MAX_PREFIX) continue
            val before = node.topK.size
            node.topK.removeAll { it.text == text }
            node.topK.add(s)
            node.topK.sortWith(SUGGESTION_ORDER)
            if (node.topK.size > k) node.topK.removeAt(node.topK.lastIndex)
            cachedItems += node.topK.size - before
            touched++
        }
        return touched
    }

    override fun suggest(prefix: String, k: Int): List<Suggestion> {
        val p = Normalizer.normalize(prefix)
        if (p.isEmpty() || p.length > MAX_PREFIX) return emptyList()
        var node = root
        for (ch in p) node = node.children[ch] ?: return emptyList()   // O(p), p <= 50 => O(1)
        return if (k >= node.topK.size) node.topK.toList() else node.topK.subList(0, k).toList()
    }

    /** Số nút + số gợi ý lưu thêm tại các nút -> thấy rõ "đánh đổi bộ nhớ lấy tốc độ". */
    override fun storageUnits(): Long = nodeCount + cachedItems
}
