# Search Autocomplete – Demo Chương 13

App Android (Kotlin + Jetpack Compose) minh họa **Chương 13: Design a Search Autocomplete System**
trong sách *System Design Interview – An Insider's Guide (2nd Edition)*.

## Cách chạy

1. Mở thư mục này bằng **Android Studio** (File → Open → chọn thư mục `source`).
2. Đợi Gradle Sync xong (lần đầu cần Internet để tải thư viện).
3. Chọn máy ảo hoặc điện thoại thật → bấm **Run ▶**.
4. Chạy unit test: chuột phải thư mục `app/src/test` → **Run 'Tests in ...'** (không cần máy ảo).

## App có 2 tab

### Tab 1 – Tìm kiếm
- Ô tìm kiếm thật, hiện **top 5 gợi ý**, in đậm phần đã gõ, bên phải là tần suất.
- Chọn **dữ liệu**: *Ví dụ trong sách* (đúng các ví dụ ở slide) hoặc *Dữ liệu lớn* (~14.000 truy vấn).
- Chọn **cách lấy gợi ý**: Quét danh sách / Trie cơ bản / Trie top-k.
- Công tắc minh họa slide 9:
  - **Debounce 300 ms** – tắt đi rồi gõ nhanh sẽ thấy *Số lần tra cứu* tăng theo từng phím.
  - **Cache theo tiền tố (LruCache)** – xóa chữ rồi gõ lại, *Lấy từ cache* tăng.
  - **Giả lập mạng chậm 400 ms** – gõ nhanh sẽ thấy *Request bị hủy* tăng (nhờ `mapLatest`).
- Nút **"Ví dụ Chương 7: cập nhật beer 10 → 30"** (chỉ hiện với dữ liệu sách): cập nhật tần suất,
  app báo phải sửa 4 nút (b, be, bee, beer) và danh sách gợi ý của "be" đổi thứ tự.

### Tab 2 – So sánh tốc độ
Chạy cùng một bộ tiền tố ngẫu nhiên (dài 1–4 ký tự) cho 3 cách, hiện biểu đồ cột:

| Cách | Ý tưởng | Độ phức tạp |
|---|---|---|
| 1. Quét danh sách | Giống SQL `LIKE 'prefix%' ORDER BY frequency` | O(N) + O(m log m) |
| 2. Trie cơ bản | Tìm nút → duyệt cây con → sắp xếp | O(p) + O(c) + O(c log c) |
| 3. Trie lưu sẵn top-k | Giới hạn tiền tố + lưu sẵn top 5 tại mỗi nút | O(1) + O(1) |

Kết quả tham khảo (chạy trên máy tính, 13.874 truy vấn, 2.000 lần thử):

| Cách | Thời gian / lần | Khối lượng lưu trữ |
|---|---|---|
| Quét danh sách | ~374 µs | 13.874 |
| Trie cơ bản | ~422 µs | 86.816 |
| Trie top-k | **~0,44 µs** (nhanh ~850 lần) | **218.860** (nhiều nhất) |

→ Đúng bài học của sách: **đánh đổi bộ nhớ lấy tốc độ**. Trie cơ bản không nhanh hơn quét danh sách
khi tiền tố ngắn vì cây con quá lớn – chính là lý do sách phải tối ưu thêm.
Số liệu trên điện thoại sẽ khác, nhưng tỉ lệ giữa các cách tương tự.

## Cấu trúc mã nguồn

```
app/src/main/java/com/example/sourcecode/
├── MainActivity.kt                 // 2 tab: Tìm kiếm, So sánh tốc độ
├── engine/                         // LÕI THUẬT TOÁN (Kotlin thuần, có unit test)
│   ├── Normalizer.kt               // chữ thường + bỏ dấu tiếng Việt
│   ├── Suggester.kt                // interface chung + Suggestion, QueryEntry
│   ├── LinearSuggester.kt          // Cách 1
│   ├── BasicTrie.kt                // Cách 2
│   ├── TopKTrie.kt                 // Cách 3 (+ upsert: ví dụ "beer" Chương 7)
│   └── Benchmark.kt                // đo thời gian, kiểm tra 3 cách cho cùng kết quả
├── data/
│   ├── QueryRepository.kt          // ví dụ trong sách + đọc assets/queries.tsv
│   └── DataHolder.kt               // nạp dữ liệu một lần, dùng chung
└── ui/
    ├── search/   SearchViewModel.kt, SearchScreen.kt       // debounce, mapLatest, LruCache
    └── benchmark/ BenchmarkViewModel.kt, BenchmarkScreen.kt
app/src/main/assets/queries.tsv     // ~14.000 truy vấn, tần suất theo phân phối Zipf
app/src/test/.../AutocompleteEngineTest.kt   // 7 unit test theo ví dụ trong sách
```

## Ánh xạ với bài thuyết trình

| Slide | Code |
|---|---|
| 3 – Bài toán khó (SQL quá chậm) | `LinearSuggester` |
| 4 – Trie cơ bản | `BasicTrie` |
| 5 – Hai tối ưu → O(1) | `TopKTrie` (`MAX_PREFIX = 50`, `topK` tại mỗi nút) |
| 6 – Cập nhật kéo theo nút tổ tiên | `TopKTrie.upsert()` + nút "beer 10 → 30" |
| 7 – Cache | `LruCache` trong `SearchViewModel` |
| 9 – Android | `debounce(300)`, `mapLatest`, `Normalizer` (bỏ dấu tiếng Việt) |

## Kịch bản demo 2 phút

1. Tab Tìm kiếm, dữ liệu sách: gõ **"be"** → [best, bet, bee, be, beer] đúng như sách.
2. Bấm **cập nhật beer 10 → 30** → "beer" nhảy lên vị trí 2, app báo sửa 4 nút.
3. Chuyển dữ liệu lớn, gõ **"pho bo"** (không dấu) → ra gợi ý có dấu "phở bò ...".
4. Tắt debounce, gõ nhanh → số lần tra cứu tăng vọt; bật lại → chỉ còn 1–2 lần.
5. Tab So sánh → **Chạy so sánh** → chỉ vào cột thời gian và cột lưu trữ.
