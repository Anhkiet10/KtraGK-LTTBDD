package com.example.sourcecode.data

import android.content.Context
import com.example.sourcecode.engine.QueryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Hai bộ dữ liệu có thể chọn trong app. */
enum class Dataset(val label: String) {
    BOOK("Ví dụ trong sách"),
    LARGE("Dữ liệu lớn (~14.000)"),
}

/** Đọc dữ liệu một lần rồi dùng chung cho cả 2 màn hình. */
object DataHolder {
    private val mutex = Mutex()
    private var large: List<QueryEntry>? = null

    suspend fun entries(context: Context, dataset: Dataset): List<QueryEntry> = when (dataset) {
        Dataset.BOOK -> QueryRepository.bookExamples
        Dataset.LARGE -> mutex.withLock {
            large ?: withContext(Dispatchers.IO) {
                QueryRepository.loadLargeDataset(context.applicationContext)
            }.also { large = it }
        }
    }
}
