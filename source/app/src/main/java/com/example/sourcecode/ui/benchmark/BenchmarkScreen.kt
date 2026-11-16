package com.example.sourcecode.ui.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcecode.data.Dataset
import com.example.sourcecode.engine.BenchmarkReport
import com.example.sourcecode.engine.BenchmarkResult
import java.util.Locale

private val barColors = listOf(Color(0xFF9E9E9E), Color(0xFF349BFF), Color(0xFFFF68D4))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BenchmarkScreen(modifier: Modifier = Modifier, vm: BenchmarkViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("So sánh 3 cách lấy top 5 gợi ý", style = MaterialTheme.typography.titleLarge)
        Text(
            "Cùng một bộ tiền tố ngẫu nhiên (dài 1–4 ký tự), đo thời gian trung bình mỗi lần gợi ý.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Dữ liệu", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dataset.entries.forEach { d ->
                FilterChip(ui.dataset == d, onClick = { vm.selectDataset(d) }, label = { Text(d.label) })
            }
        }
        Text("Số lần thử", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(500, 1000, 5000).forEach { n ->
                FilterChip(ui.queries == n, onClick = { vm.selectQueries(n) }, label = { Text("$n") })
            }
        }

        Button(onClick = vm::run, enabled = !ui.running, modifier = Modifier.fillMaxWidth()) {
            if (ui.running) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Đang chạy…")
            } else {
                Text("Chạy so sánh")
            }
        }
        ui.error?.let { Text("Lỗi: $it", color = MaterialTheme.colorScheme.error) }
        ui.report?.let { ReportView(it) }
    }
}

@Composable
private fun ReportView(report: BenchmarkReport) {
    val maxTime = report.results.maxOf { it.avgMicros }.coerceAtLeast(1e-9)
    val maxStorage = report.results.maxOf { it.storageUnits }.coerceAtLeast(1L)
    val baseline = report.results.first().avgMicros

    Text(
        "${report.datasetSize} truy vấn • ${report.queries} lần thử • " +
            if (report.allSame) "3 cách cho kết quả giống nhau ✓" else "Kết quả KHÁC nhau ✗",
        style = MaterialTheme.typography.labelLarge,
        color = if (report.allSame) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )

    Text("Thời gian trung bình mỗi lần gợi ý", fontWeight = FontWeight.Bold)
    report.results.forEachIndexed { i, r ->
        val speedup = if (r.avgMicros > 0) baseline / r.avgMicros else 0.0
        BarRow(
            title = r.name,
            subtitle = r.complexity,
            value = fmt(r.avgMicros) + " µs" + if (i > 0) "  (nhanh ×${fmt(speedup)})" else "",
            fraction = (r.avgMicros / maxTime).toFloat(),
            color = barColors[i],
        )
    }

    Text("Khối lượng lưu trữ (số nút + phần tử lưu thêm)", fontWeight = FontWeight.Bold)
    report.results.forEachIndexed { i, r ->
        BarRow(
            title = r.name,
            subtitle = "Xây trong ${r.buildMillis} ms",
            value = "%,d".format(Locale.US, r.storageUnits),
            fraction = r.storageUnits.toFloat() / maxStorage,
            color = barColors[i],
        )
    }

    Card(Modifier.fillMaxWidth()) {
        Text(
            "Nhận xét: Trie lưu sẵn top-k nhanh nhất vì chỉ cần đi xuống nút tiền tố rồi đọc danh sách " +
                "có sẵn, nhưng tốn bộ nhớ nhất và xây cây lâu hơn – đúng bài học \"đánh đổi bộ nhớ lấy tốc độ\". " +
                "Hệ thống đọc nhiều hơn ghi rất nhiều nên đánh đổi này xứng đáng.",
            Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BarRow(title: String, subtitle: String, value: String, fraction: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.labelLarge)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))   // tối thiểu 2% để vẫn nhìn thấy
                    .height(14.dp)
                    .background(color, RoundedCornerShape(7.dp)),
            )
        }
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

private fun fmt(x: Double): String = String.format(Locale.US, if (x >= 100) "%.0f" else "%.2f", x)
