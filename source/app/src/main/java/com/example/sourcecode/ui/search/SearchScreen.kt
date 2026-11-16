package com.example.sourcecode.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcecode.data.Dataset
import com.example.sourcecode.engine.Normalizer
import com.example.sourcecode.engine.Suggestion
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(modifier: Modifier = Modifier, vm: SearchViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()

    // Toàn bộ màn hình là một LazyColumn: phần điều khiển ở trên, danh sách gợi ý ở dưới
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Column {
                // --- Chọn dữ liệu và cách tra cứu ---
                Text("Dữ liệu", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dataset.entries.forEach { d ->
                        FilterChip(
                            selected = ui.dataset == d,
                            onClick = { vm.loadDataset(d) },
                            label = { Text(d.label) },
                        )
                    }
                }
                Text("Cách lấy gợi ý", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EngineType.entries.forEach { e ->
                        FilterChip(
                            selected = ui.engine == e,
                            onClick = { vm.selectEngine(e) },
                            label = { Text(e.label) },
                        )
                    }
                }

                // --- Các kỹ thuật phía app di động (slide 9) ---
                SwitchRow("Debounce 300 ms", ui.debounceOn, vm::setDebounce)
                SwitchRow("Cache theo tiền tố (LruCache)", ui.cacheOn, vm::setCache)
                SwitchRow("Giả lập mạng chậm 400 ms (thấy mapLatest hủy)", ui.slowNetworkOn, vm::setSlowNetwork)

                Spacer(Modifier.padding(4.dp))
                OutlinedTextField(
                    value = ui.query,
                    onValueChange = vm::onQueryChange,
                    label = { Text("Tìm kiếm (thử gõ \"pho bo\", \"be\", \"tr\")") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))

                StatsCard(ui, onReset = vm::resetStats)

                if (ui.dataset == Dataset.BOOK) {
                    OutlinedButton(onClick = vm::updateBeerExample, modifier = Modifier.fillMaxWidth()) {
                        Text("Ví dụ Chương 7: cập nhật \"beer\" 10 → 30")
                    }
                }
                ui.message?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (ui.query.isNotBlank() && ui.suggestions.isEmpty() && !ui.loading) {
                    Text("Không có gợi ý", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        items(ui.suggestions, key = { it.text }) { s ->
            SuggestionRow(s, ui.query, onClick = { vm.onQueryChange(s.text) })
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StatsCard(ui: SearchUiState, onReset: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Thống kê (${ui.datasetSize} truy vấn)",
                    Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onReset) { Text("Đặt lại") }
            }
            Text("Số lần gõ phím: ${ui.keystrokes}   •   Số lần tra cứu: ${ui.lookups}")
            Text("Lấy từ cache: ${ui.cacheHits}   •   Request bị hủy: ${ui.cancelled}")
            val t = ui.lastLookupMicros?.let { String.format(Locale.US, "%.1f µs", it) } ?: "–"
            Text("Thời gian tra cứu gần nhất: $t")
        }
    }
}

/** Một dòng gợi ý: in đậm phần người dùng đã gõ, bên phải là tần suất. */
@Composable
private fun SuggestionRow(s: Suggestion, typed: String, onClick: () -> Unit) {
    // Chuỗi bỏ dấu có cùng độ dài với chuỗi gốc, nên in đậm đúng số ký tự đã gõ
    val n = minOf(Normalizer.normalize(typed).length, s.text.length)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s.text.take(n)) }
                append(s.text.drop(n))
            },
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            s.frequency.toString(),
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
    HorizontalDivider()
}
