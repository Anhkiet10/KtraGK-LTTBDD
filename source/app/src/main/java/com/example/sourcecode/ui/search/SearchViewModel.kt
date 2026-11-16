package com.example.sourcecode.ui.search

import android.app.Application
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcecode.data.DataHolder
import com.example.sourcecode.data.Dataset
import com.example.sourcecode.engine.BasicTrie
import com.example.sourcecode.engine.LinearSuggester
import com.example.sourcecode.engine.Normalizer
import com.example.sourcecode.engine.Suggester
import com.example.sourcecode.engine.Suggestion
import com.example.sourcecode.engine.TopKTrie
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Cách lấy gợi ý đang dùng trên màn hình Tìm kiếm. */
enum class EngineType(val label: String) { LINEAR("Quét DS"), BASIC("Trie cơ bản"), TOPK("Trie top-k") }

data class SearchUiState(
    val query: String = "",
    val suggestions: List<Suggestion> = emptyList(),
    val dataset: Dataset = Dataset.BOOK,
    val engine: EngineType = EngineType.TOPK,
    val loading: Boolean = false,
    val datasetSize: Int = 0,
    // Các công tắc minh họa slide 9
    val debounceOn: Boolean = true,
    val cacheOn: Boolean = true,
    val slowNetworkOn: Boolean = false,
    // Thống kê để thấy hiệu quả của từng kỹ thuật
    val keystrokes: Int = 0,
    val lookups: Int = 0,
    val cacheHits: Int = 0,
    val cancelled: Int = 0,
    val lastLookupMicros: Double? = null,
    val message: String? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui.asStateFlow()

    /** Chuỗi người dùng gõ – nguồn của luồng xử lý gợi ý. */
    private val queryFlow = MutableStateFlow("")

    /** Bộ đệm phía client theo tiền tố (giống browser cache trong sách). */
    private val cache = LruCache<String, List<Suggestion>>(200)

    private var engines: Map<EngineType, Suggester> = emptyMap()
    private var topK: TopKTrie? = null

    init {
        loadDataset(Dataset.BOOK)

        viewModelScope.launch {
            queryFlow
                // Debounce: chỉ xử lý khi người dùng ngừng gõ 300 ms (tắt công tắc => 0 ms)
                .debounce { if (_ui.value.debounceOn) 300L else 0L }
                .distinctUntilChanged()
                // mapLatest: có chữ mới thì HỦY lần tra cứu cũ đang chạy dở
                .mapLatest { q -> lookup(q) }
                .collect { result ->
                    _ui.update { it.copy(suggestions = result) }
                }
        }
    }

    fun onQueryChange(text: String) {
        _ui.update { it.copy(query = text, keystrokes = it.keystrokes + 1) }
        queryFlow.value = text
    }

    private suspend fun lookup(q: String): List<Suggestion> {
        val key = Normalizer.normalize(q)
        if (key.isEmpty()) return emptyList()
        val state = _ui.value
        val cacheKey = "${state.dataset}|${state.engine}|$key"

        if (state.cacheOn) {
            cache.get(cacheKey)?.let { hit ->
                _ui.update { it.copy(cacheHits = it.cacheHits + 1) }
                return hit
            }
        }
        try {
            // Giả lập gọi API qua mạng chậm, để thấy mapLatest hủy request cũ
            if (state.slowNetworkOn) delay(400)
        } catch (e: CancellationException) {
            _ui.update { it.copy(cancelled = it.cancelled + 1) }
            throw e
        }
        val engine = engines[state.engine] ?: return emptyList()
        val start = System.nanoTime()
        val result = withContext(Dispatchers.Default) { engine.suggest(key, 5) }
        val micros = (System.nanoTime() - start) / 1000.0
        cache.put(cacheKey, result)
        _ui.update { it.copy(lookups = it.lookups + 1, lastLookupMicros = micros) }
        return result
    }

    fun loadDataset(dataset: Dataset) {
        _ui.update { it.copy(dataset = dataset, loading = true, message = null) }
        viewModelScope.launch {
            val entries = DataHolder.entries(getApplication<Application>(), dataset)
            val built = withContext(Dispatchers.Default) {
                val t = TopKTrie(entries)
                topK = t
                mapOf(
                    EngineType.LINEAR to LinearSuggester(entries),
                    EngineType.BASIC to BasicTrie(entries),
                    EngineType.TOPK to t,
                )
            }
            engines = built
            cache.evictAll()
            _ui.update { it.copy(loading = false, datasetSize = entries.size) }
            refresh()
        }
    }

    fun selectEngine(type: EngineType) {
        _ui.update { it.copy(engine = type) }
        refresh()
    }

    fun setDebounce(on: Boolean) = _ui.update { it.copy(debounceOn = on) }
    fun setCache(on: Boolean) { cache.evictAll(); _ui.update { it.copy(cacheOn = on) } }
    fun setSlowNetwork(on: Boolean) = _ui.update { it.copy(slowNetworkOn = on) }

    fun resetStats() = _ui.update {
        it.copy(keystrokes = 0, lookups = 0, cacheHits = 0, cancelled = 0, lastLookupMicros = null)
    }

    /** Ví dụ Chương 7 của sách: tần suất "beer" tăng 10 -> 30, các nút tổ tiên phải cập nhật. */
    fun updateBeerExample() {
        val trie = topK ?: return
        val touched = trie.upsert("beer", 30)
        cache.evictAll()
        _ui.update {
            it.copy(
                engine = EngineType.TOPK,
                query = "be",
                message = "Đã cập nhật \"beer\" = 30: phải sửa top-k của $touched nút (b, be, bee, beer)",
            )
        }
        queryFlow.value = "be"
        refresh()
    }

    /** Tra lại ngay với chuỗi hiện tại (khi đổi dữ liệu / đổi cách). */
    private fun refresh() {
        viewModelScope.launch {
            val result = lookup(_ui.value.query)
            _ui.update { it.copy(suggestions = result) }
        }
    }
}
