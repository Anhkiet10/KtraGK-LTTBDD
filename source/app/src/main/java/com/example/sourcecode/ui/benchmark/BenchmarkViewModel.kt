package com.example.sourcecode.ui.benchmark

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcecode.data.DataHolder
import com.example.sourcecode.data.Dataset
import com.example.sourcecode.engine.Benchmark
import com.example.sourcecode.engine.BenchmarkReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BenchmarkUiState(
    val dataset: Dataset = Dataset.LARGE,
    val queries: Int = 1000,
    val running: Boolean = false,
    val report: BenchmarkReport? = null,
    val error: String? = null,
)

class BenchmarkViewModel(app: Application) : AndroidViewModel(app) {
    private val _ui = MutableStateFlow(BenchmarkUiState())
    val ui: StateFlow<BenchmarkUiState> = _ui.asStateFlow()

    fun selectDataset(d: Dataset) = _ui.update { it.copy(dataset = d, report = null) }
    fun selectQueries(n: Int) = _ui.update { it.copy(queries = n, report = null) }

    fun run() {
        if (_ui.value.running) return
        _ui.update { it.copy(running = true, error = null) }
        viewModelScope.launch {
            try {
                val state = _ui.value
                val entries = DataHolder.entries(getApplication<Application>(), state.dataset)
                // Chạy trên luồng nền để không làm đơ giao diện
                val report = withContext(Dispatchers.Default) { Benchmark.run(entries, state.queries) }
                _ui.update { it.copy(running = false, report = report) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(running = false, error = e.message ?: e.toString()) }
            }
        }
    }
}
