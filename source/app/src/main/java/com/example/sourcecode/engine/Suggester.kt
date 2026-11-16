package com.example.sourcecode.engine

/** Một gợi ý: chuỗi hiển thị (có dấu) và số lần được tìm kiếm. */
data class Suggestion(val text: String, val frequency: Long)

/** Một dòng dữ liệu đầu vào: truy vấn gốc và tần suất. */
data class QueryEntry(val text: String, val frequency: Long) {
    /** Khóa đã chuẩn hóa (chữ thường, bỏ dấu) dùng để tra cứu. */
    val key: String = Normalizer.normalize(text)
}

/**
 * Giao diện chung cho 3 cách lấy gợi ý, để màn hình So sánh đo cùng một bài toán:
 * cho một tiền tố, trả về k truy vấn có tần suất cao nhất bắt đầu bằng tiền tố đó.
 */
interface Suggester {
    /** Tên hiển thị trên màn hình so sánh. */
    val name: String

    /** Mô tả ngắn độ phức tạp, ứng với slide trong bài thuyết trình. */
    val complexity: String

    /** Trả về tối đa [k] gợi ý cho [prefix] (prefix có thể chưa chuẩn hóa). */
    fun suggest(prefix: String, k: Int = 5): List<Suggestion>

    /** Ước lượng "khối lượng lưu trữ": số nút + số phần tử lưu thêm. Dùng để so sánh bộ nhớ. */
    fun storageUnits(): Long
}

/** Thứ tự chuẩn: tần suất giảm dần, cùng tần suất thì theo bảng chữ cái (để kết quả ổn định). */
val SUGGESTION_ORDER: Comparator<Suggestion> =
    compareByDescending<Suggestion> { it.frequency }.thenBy { it.text }
