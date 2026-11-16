package com.example.sourcecode.data

import android.content.Context
import com.example.sourcecode.engine.QueryEntry

/**
 * Nguồn dữ liệu cho app.
 *  - [bookExamples]: đúng các ví dụ trong Chương 13 của sách, để demo khớp với slide.
 *  - [loadLargeDataset]: ~14.000 truy vấn tiếng Việt / tiếng Anh trong assets/queries.tsv,
 *    tần suất sinh theo phân phối Zipf (ít truy vấn rất phổ biến, đa số hiếm) – dùng để so sánh tốc độ.
 */
object QueryRepository {

    /** Ví dụ trong sách: bảng 4.1 (tree, try, true, toy, wish, win), nút "be", bảng 3.2 (tw...). */
    val bookExamples: List<QueryEntry> = listOf(
        "tree" to 10L, "try" to 29L, "true" to 35L, "toy" to 14L, "wish" to 25L, "win" to 50L,
        "be" to 15L, "bee" to 20L, "beer" to 10L, "best" to 35L, "bet" to 29L,
        "twitter" to 35L, "twitch" to 29L, "twilight" to 25L, "twin peak" to 21L,
        "twitch prime" to 18L, "twitter search" to 14L, "twillo" to 10L, "twin peak sf" to 8L,
        "dinner" to 40L, "dinner ideas" to 33L, "dinner recipes" to 27L, "dinner near me" to 22L,
        "dinosaur" to 12L,
        "phở bò" to 60L, "phở bò Nam Định" to 41L, "phở bò Hà Nội" to 38L, "phở gà" to 30L,
        "bún chả" to 26L, "bún bò Huế" to 24L, "bánh mì" to 45L,
    ).map { (text, freq) -> QueryEntry(text, freq) }

    /** Đọc file TSV: mỗi dòng "truy vấn<TAB>tần suất". Gọi trên luồng IO. */
    fun loadLargeDataset(context: Context, fileName: String = "queries.tsv"): List<QueryEntry> =
        context.assets.open(fileName).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.mapNotNull { line ->
                val tab = line.lastIndexOf('\t')
                if (tab <= 0) return@mapNotNull null
                val freq = line.substring(tab + 1).trim().toLongOrNull() ?: return@mapNotNull null
                QueryEntry(line.substring(0, tab), freq)
            }.toList()
        }
}
