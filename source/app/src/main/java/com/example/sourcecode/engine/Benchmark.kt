package com.example.sourcecode.engine

import kotlin.random.Random

/** Kết quả đo của một cách. */
data class BenchmarkResult(
    val name: String,
    val complexity: String,
    val avgMicros: Double,      // thời gian trung bình mỗi lần gợi ý (micro giây)
    val buildMillis: Long,      // thời gian xây cấu trúc dữ liệu
    val storageUnits: Long,     // ước lượng khối lượng lưu trữ
)

data class BenchmarkReport(
    val results: List<BenchmarkResult>,
    val queries: Int,
    val datasetSize: Int,
    val allSame: Boolean,       // 3 cách có trả về cùng kết quả không (kiểm tra tính đúng)
)

/**
 * So sánh 3 cách trên cùng một bộ tiền tố ngẫu nhiên.
 * Tiền tố được lấy từ chính dữ liệu, dài 1–4 ký tự (giống lúc người dùng mới bắt đầu gõ),
 * đây là trường hợp bất lợi nhất cho cách 1 và cách 2.
 */
object Benchmark {

    fun randomPrefixes(entries: List<QueryEntry>, count: Int, seed: Int = 13): List<String> {
        val rnd = Random(seed)
        return List(count) {
            val key = entries[rnd.nextInt(entries.size)].key
            key.take(1 + rnd.nextInt(minOf(4, key.length)))
        }
    }

    /** Đo một cách: chạy làm nóng (warm-up) rồi đo [prefixes.size] lần. */
    fun measure(s: Suggester, prefixes: List<String>, buildMillis: Long): BenchmarkResult {
        repeat(200) { s.suggest(prefixes[it % prefixes.size]) }        // warm-up cho JIT
        var sink = 0
        val start = System.nanoTime()
        for (p in prefixes) sink += s.suggest(p).size
        val elapsed = System.nanoTime() - start
        if (sink < 0) println(sink)                                    // tránh bị tối ưu bỏ đi
        return BenchmarkResult(
            name = s.name,
            complexity = s.complexity,
            avgMicros = elapsed / 1000.0 / prefixes.size,
            buildMillis = buildMillis,
            storageUnits = s.storageUnits(),
        )
    }

    /** Xây 3 cấu trúc, đo thời gian xây, đo thời gian truy vấn và kiểm tra kết quả giống nhau. */
    fun run(entries: List<QueryEntry>, queries: Int = 1000): BenchmarkReport {
        val (linear, tLinear) = timed { LinearSuggester(entries) }
        val (basic, tBasic) = timed { BasicTrie(entries) }
        val (topK, tTopK) = timed { TopKTrie(entries) }

        val prefixes = randomPrefixes(entries, queries)
        val allSame = prefixes.take(200).all { p ->
            val a = linear.suggest(p)
            a == basic.suggest(p) && a == topK.suggest(p)
        }
        return BenchmarkReport(
            results = listOf(
                measure(linear, prefixes, tLinear),
                measure(basic, prefixes, tBasic),
                measure(topK, prefixes, tTopK),
            ),
            queries = queries,
            datasetSize = entries.size,
            allSame = allSame,
        )
    }

    private inline fun <T> timed(block: () -> T): Pair<T, Long> {
        val start = System.nanoTime()
        val value = block()
        return value to (System.nanoTime() - start) / 1_000_000
    }
}
