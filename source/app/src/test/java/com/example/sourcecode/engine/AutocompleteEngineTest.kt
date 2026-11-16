package com.example.sourcecode.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Kiểm tra lõi thuật toán bằng đúng các ví dụ trong Chương 13 của sách.
 * Chạy: chuột phải thư mục test -> Run 'Tests in ...' (không cần máy ảo).
 */
class AutocompleteEngineTest {

    private val book = listOf(
        "tree" to 10L, "try" to 29L, "true" to 35L, "toy" to 14L, "wish" to 25L, "win" to 50L,
        "be" to 15L, "bee" to 20L, "beer" to 10L, "best" to 35L, "bet" to 29L,
    ).map { QueryEntry(it.first, it.second) }

    private fun texts(list: List<Suggestion>) = list.map { it.text }

    @Test
    fun trie_tr_k2_matchesBook() {
        // Sách: gõ "tr", k = 2 -> [true: 35, try: 29]
        val expected = listOf("true", "try")
        assertEquals(expected, texts(LinearSuggester(book).suggest("tr", 2)))
        assertEquals(expected, texts(BasicTrie(book).suggest("tr", 2)))
        assertEquals(expected, texts(TopKTrie(book).suggest("tr", 2)))
    }

    @Test
    fun topK_nodeBe_matchesBook() {
        // Sách: nút "be" lưu [best: 35, bet: 29, bee: 20, be: 15, beer: 10]
        val result = TopKTrie(book).suggest("be")
        assertEquals(listOf("best", "bet", "bee", "be", "beer"), texts(result))
        assertEquals(listOf(35L, 29L, 20L, 15L, 10L), result.map { it.frequency })
    }

    @Test
    fun update_beer_touchesAllAncestors() {
        // Sách Chương 7: "beer" 10 -> 30, các nút b, be, bee, beer đều phải cập nhật
        val trie = TopKTrie(book)
        val touched = trie.upsert("beer", 30)
        assertEquals(4, touched)
        assertEquals(listOf("best", "beer", "bet", "bee", "be"), texts(trie.suggest("be")))
        assertEquals(listOf("beer"), texts(trie.suggest("bee", 1)))
    }

    @Test
    fun normalizer_removesVietnameseMarks() {
        assertEquals("pho bo", Normalizer.normalize("Phở Bò"))
        assertEquals("duong pho", Normalizer.normalize("  Đường   phố "))
        assertEquals("bun bo hue", Normalizer.normalize("bún bò Huế"))
    }

    @Test
    fun vietnamese_typeWithoutMarks_getsSuggestionWithMarks() {
        val data = listOf(QueryEntry("phở bò", 60), QueryEntry("phở bò Nam Định", 41), QueryEntry("phở gà", 30))
        val result = TopKTrie(data).suggest("pho bo")
        assertEquals(listOf("phở bò", "phở bò Nam Định"), texts(result))
    }

    @Test
    fun prefixLongerThanLimit_returnsEmpty() {
        val long = "a".repeat(TopKTrie.MAX_PREFIX + 1)
        assertTrue(TopKTrie(book).suggest(long).isEmpty())
    }

    @Test
    fun threeEngines_giveSameResults_onRandomData() {
        val rnd = Random(7)
        val alphabet = "abcde"
        val data = (1..3000).map {
            val word = (1..(1 + rnd.nextInt(6))).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
            word to rnd.nextLong(1, 1000)
        }.distinctBy { it.first }.map { QueryEntry(it.first, it.second) }

        val linear = LinearSuggester(data)
        val basic = BasicTrie(data)
        val topK = TopKTrie(data)
        for (p in Benchmark.randomPrefixes(data, 500)) {
            val expected = linear.suggest(p)
            assertEquals("prefix=$p", expected, basic.suggest(p))
            assertEquals("prefix=$p", expected, topK.suggest(p))
        }
    }
}
