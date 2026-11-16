package com.example.sourcecode.engine

import java.text.Normalizer as JNormalizer

/**
 * Chuẩn hóa truy vấn trước khi đưa vào cây Trie (mở rộng tiếng Việt – Chương 9 báo cáo):
 *  - chuyển về chữ thường
 *  - bỏ dấu tiếng Việt ("phở bò" -> "pho bo", "đường" -> "duong")
 *  - gộp khoảng trắng thừa
 *
 * Khóa đã chuẩn hóa dùng để TRA CỨU; chuỗi gốc có dấu vẫn được giữ để HIỂN THỊ.
 */
object Normalizer {
    private val diacritics = Regex("\\p{Mn}+")
    private val spaces = Regex("\\s+")

    fun normalize(input: String): String {
        val lower = input.trim().lowercase()
        val noMarks = JNormalizer.normalize(lower, JNormalizer.Form.NFD).replace(diacritics, "")
        return noMarks
            .replace('đ', 'd')
            .replace(spaces, " ")
    }
}
