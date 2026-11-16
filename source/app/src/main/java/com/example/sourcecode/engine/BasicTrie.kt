package com.example.sourcecode.engine

/**
 * CÁCH 2 – Cây Trie cơ bản (slide 4 / mục 4.4 báo cáo).
 *
 * Bước 1: đi từ gốc theo từng ký tự để tìm nút tiền tố  -> O(p)
 * Bước 2: duyệt toàn bộ cây con để lấy các truy vấn hợp lệ -> O(c)
 * Bước 3: sắp xếp theo tần suất và lấy k phần tử đầu     -> O(c log c)
 *
 * Đúng nhưng chậm khi tiền tố ngắn (ví dụ gõ "a"), vì cây con có thể rất lớn.
 */
class BasicTrie(entries: List<QueryEntry>) : Suggester {
    override val name = "2. Trie cơ bản"
    override val complexity = "O(p) + O(c) + O(c log c)"

    private class Node {
        val children = HashMap<Char, Node>()   // HashMap để chứa cả ký tự Unicode
        val words = ArrayList<Suggestion>(0)   // các truy vấn kết thúc đúng tại nút này
    }

    private val root = Node()
    private var nodeCount = 1L

    init {
        entries.forEach { insert(it) }
    }

    fun insert(entry: QueryEntry) {
        var node = root
        for (ch in entry.key) {
            node = node.children.getOrPut(ch) { nodeCount++; Node() }
        }
        node.words.add(Suggestion(entry.text, entry.frequency))
    }

    override fun suggest(prefix: String, k: Int): List<Suggestion> {
        val p = Normalizer.normalize(prefix)
        if (p.isEmpty()) return emptyList()

        // Bước 1: tìm nút tiền tố
        var node = root
        for (ch in p) node = node.children[ch] ?: return emptyList()

        // Bước 2: duyệt cây con (dùng ngăn xếp thay cho đệ quy để tránh tràn stack)
        val collected = ArrayList<Suggestion>()
        val stack = ArrayDeque<Node>()
        stack.addLast(node)
        while (stack.isNotEmpty()) {
            val n = stack.removeLast()
            collected.addAll(n.words)
            stack.addAll(n.children.values)
        }

        // Bước 3: sắp xếp và lấy top k
        collected.sortWith(SUGGESTION_ORDER)
        return collected.take(k)
    }

    override fun storageUnits(): Long = nodeCount
}
