package com.example.sourcecode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sourcecode.ui.benchmark.BenchmarkScreen
import com.example.sourcecode.ui.search.SearchScreen
import com.example.sourcecode.ui.theme.SourcecodeTheme

/**
 * Demo Chương 13 – Design a Search Autocomplete System.
 *  Tab 1 "Tìm kiếm": ô gợi ý thật + debounce / cache / hủy request cũ.
 *  Tab 2 "So sánh": đo tốc độ 3 cách (quét danh sách, Trie cơ bản, Trie lưu sẵn top-k).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SourcecodeTheme {
                AutocompleteApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf("Tìm kiếm", "So sánh tốc độ")

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            Text(
                "Search Autocomplete",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            )
            PrimaryTabRow(selectedTabIndex = tab) {
                titles.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> SearchScreen(Modifier.padding(top = 8.dp))
                else -> BenchmarkScreen()
            }
        }
    }
}
