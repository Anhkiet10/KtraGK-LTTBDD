# 🔎 Search Autocomplete – Demo Chương 13 *System Design Interview*

> App Android (Kotlin + Jetpack Compose) cho thấy **vì sao ô gợi ý của Google trả kết quả gần như tức thì**:
> từ cách làm đơn giản nhưng chậm (quét danh sách như SQL) → cây **Trie** → **Trie lưu sẵn top-k** chạy **O(1)**,
> kèm các kỹ thuật phía app: **debounce**, **hủy request cũ**, **cache**, **gõ không dấu tiếng Việt**.

Đề tài giữa kỳ môn **Lập trình thiết bị di động**, dựa trên *System Design Interview – An Insider's Guide (2nd Edition)*,
Alex Xu, **Chương 13: Design a Search Autocomplete System**.

---

## Mục lục

- [Tóm tắt trong 30 giây](#tóm-tắt-trong-30-giây)
- [Tính năng](#tính-năng)
- [Kết quả so sánh](#kết-quả-so-sánh)
- [Chạy thử](#chạy-thử)
- [Cấu trúc mã nguồn](#cấu-trúc-mã-nguồn)
- [Thuật toán hoạt động thế nào](#thuật-toán-hoạt-động-thế-nào)
- [Kỹ thuật phía app Android](#kỹ-thuật-phía-app-android)
- [Dữ liệu](#dữ-liệu)
- [Ánh xạ với nội dung sách](#ánh-xạ-với-nội-dung-sách)
- [Kịch bản demo 2 phút](#kịch-bản-demo-2-phút)
- [Tác giả và tài liệu tham khảo](#tác-giả-và-tài-liệu-tham-khảo)

---

## Tóm tắt trong 30 giây

**Bài toán:** người dùng gõ một tiền tố (ví dụ `"be"`), hệ thống trả về **5 truy vấn được tìm nhiều nhất**
bắt đầu bằng tiền tố đó, trong vòng **dưới 100 ms**, dù mỗi giây có hàng chục nghìn lượt gõ phím.

**Chương 13 giải bài toán bằng cách tối ưu dần:**

| Bước | Cách làm | Vấn đề / cải tiến |
|---|---|---|
| 1 | Bảng tần suất + `SELECT … LIKE 'be%' ORDER BY frequency DESC LIMIT 5` | Quét cả bảng mỗi lần gõ → quá chậm |
| 2 | Cây **Trie** cơ bản: tìm nút tiền tố, duyệt cây con, sắp xếp | Tiền tố ngắn thì cây con rất lớn → vẫn chậm |
| 3 | **Trie tối ưu**: giới hạn độ dài tiền tố + **lưu sẵn top-k tại mỗi nút** | Tra cứu **O(1)**, đổi lại tốn thêm bộ nhớ |
| 4 | Kiến trúc: không cập nhật real-time, xây lại cây theo tuần, cache, phân mảnh | Hệ thống chịu được tải lớn |

App này **cài đặt cả 3 cách ở bước 1–3**, đo tốc độ trên cùng dữ liệu và kiểm tra cả 3 cho **cùng kết quả**.

---

## Tính năng

### Tab 1 – Tìm kiếm

- Ô tìm kiếm hiện **top 5 gợi ý**, in đậm phần đã gõ, bên phải là tần suất.
- Hai bộ dữ liệu:
  - **Ví dụ trong sách**: gõ `be` ra đúng `[best, bet, bee, be, beer]` như trong sách.
  - **Dữ liệu lớn**: khoảng 14.000 truy vấn tiếng Việt và tiếng Anh.
- Đổi qua lại **3 cách lấy gợi ý** để thấy thời gian tra cứu khác nhau.
- **3 công tắc** minh họa kỹ thuật phía app: Debounce 300 ms, Cache theo tiền tố, Giả lập mạng chậm.
- **Bảng thống kê**: số lần gõ phím, số lần tra cứu, lấy từ cache, request bị hủy, thời gian tra cứu gần nhất.
- Gõ **không dấu** `pho bo` vẫn ra gợi ý **có dấu** `phở bò …`.
- Nút **"cập nhật beer 10 → 30"** tái hiện ví dụ cập nhật ở Chương 7 của báo cáo: app báo phải sửa 4 nút (`b`, `be`, `bee`, `beer`).

### Tab 2 – So sánh tốc độ

- Chạy **500 / 1.000 / 5.000** tiền tố ngẫu nhiên (dài 1–4 ký tự, trường hợp khó nhất) cho cả 3 cách.
- Biểu đồ cột: **thời gian trung bình mỗi lần gợi ý** và **khối lượng lưu trữ**.
- Tự kiểm tra **3 cách cho kết quả giống nhau ✓**.

---

## Kết quả so sánh

Đo trên máy tính (JVM), dữ liệu lớn 13.874 truy vấn, 2.000 tiền tố ngẫu nhiên:

| Cách | Độ phức tạp | Thời gian / lần | Khối lượng lưu trữ* |
|---|---|---|---|
| 1. Quét danh sách (như SQL `LIKE`) | O(N) + O(m log m) | ~374 µs | 13.874 |
| 2. Trie cơ bản | O(p) + O(c) + O(c log c) | ~422 µs | 86.816 |
| 3. Trie lưu sẵn top-k | O(1) + O(1) | **~0,44 µs** (nhanh **~850 lần**) | **218.860** (nhiều nhất) |

\* Số phần tử lưu trữ: cách 1 là số dòng; cách 2 là số nút; cách 3 là số nút cộng số gợi ý lưu sẵn tại các nút.

**Đọc kết quả thế nào:**

- **Trie top-k nhanh nhất nhưng tốn bộ nhớ nhất.** Đây đúng là bài học của sách: *đánh đổi bộ nhớ lấy tốc độ*.
  Đánh đổi này đáng giá vì hệ thống **đọc nhiều hơn ghi rất nhiều**.
- **Trie cơ bản không nhanh hơn quét danh sách** khi tiền tố ngắn (`"a"`, `"ph"`…), vì vẫn phải duyệt cả cây con rất lớn.
  Đây chính là lý do sách phải tối ưu thêm.
- Trên điện thoại số liệu sẽ khác, nhưng **tỉ lệ giữa các cách tương tự**.

---

## Chạy thử

### Yêu cầu

| | Phiên bản trong project |
|---|---|
| Android Studio | Bản mới, hỗ trợ Android Gradle Plugin 9.4 |
| JDK | 17 trở lên (dùng JDK đi kèm Android Studio là được) |
| Android Gradle Plugin / Gradle | 9.4.1 / 9.6.0 |
| Kotlin | 2.2.10 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 (Android 7.0 trở lên) |

### Các bước

1. Clone hoặc tải repo về, mở thư mục gốc bằng **Android Studio** (*File → Open*).
2. Đợi **Gradle Sync** xong. Lần đầu cần Internet để tải thư viện.
3. Chọn máy ảo hoặc điện thoại thật, bấm **Run ▶**.

### Chạy unit test (không cần máy ảo)

- Trong Android Studio: chuột phải thư mục `app/src/test` → **Run 'Tests in …'**
- Hoặc dòng lệnh:

```bash
# Windows
gradlew.bat test
# macOS / Linux
./gradlew test
```

Có **7 test** trong [`AutocompleteEngineTest.kt`](app/src/test/java/com/example/sourcecode/engine/AutocompleteEngineTest.kt), kiểm tra bằng đúng ví dụ trong sách:

| Test | Kiểm tra |
|---|---|
| `trie_tr_k2_matchesBook` | Gõ `tr`, k = 2 → `[true, try]` với cả 3 cách |
| `topK_nodeBe_matchesBook` | Nút `be` lưu `[best 35, bet 29, bee 20, be 15, beer 10]` |
| `update_beer_touchesAllAncestors` | `beer` 10 → 30 phải sửa 4 nút, `beer` lên vị trí 2 |
| `normalizer_removesVietnameseMarks` | `"Phở Bò"` → `"pho bo"`, `"Đường"` → `"duong"` |
| `vietnamese_typeWithoutMarks_getsSuggestionWithMarks` | Gõ `pho bo` ra `phở bò`, `phở bò Nam Định` |
| `prefixLongerThanLimit_returnsEmpty` | Tiền tố dài hơn 50 ký tự → không gợi ý |
| `threeEngines_giveSameResults_onRandomData` | 3 cách cho cùng kết quả trên 500 tiền tố ngẫu nhiên |

---

## Cấu trúc mã nguồn

```
app/src/main/java/com/example/sourcecode/
├── MainActivity.kt              # Giao diện 2 tab: Tìm kiếm, So sánh tốc độ
│
├── engine/                      # ⭐ LÕI THUẬT TOÁN – Kotlin thuần, không phụ thuộc Android
│   ├── Suggester.kt             #    interface chung + Suggestion, QueryEntry
│   ├── LinearSuggester.kt       #    Cách 1: quét danh sách (như SQL LIKE)
│   ├── BasicTrie.kt             #    Cách 2: Trie cơ bản
│   ├── TopKTrie.kt              #    Cách 3: Trie lưu sẵn top-k (+ upsert cập nhật tần suất)
│   ├── Normalizer.kt            #    chữ thường + bỏ dấu tiếng Việt
│   └── Benchmark.kt             #    đo thời gian, kiểm tra 3 cách cho cùng kết quả
│
├── data/
│   ├── QueryRepository.kt       # ví dụ trong sách + đọc assets/queries.tsv
│   └── DataHolder.kt            # nạp dữ liệu một lần, dùng chung cho 2 tab
│
└── ui/
    ├── search/                  # Tab 1: SearchViewModel (debounce, mapLatest, LruCache) + SearchScreen
    ├── benchmark/               # Tab 2: BenchmarkViewModel + BenchmarkScreen (biểu đồ cột)
    └── theme/                   # màu, font (mặc định của Android Studio)

app/src/main/assets/queries.tsv  # ~14.000 truy vấn mẫu: "truy vấn<TAB>tần suất"
app/src/test/…/engine/AutocompleteEngineTest.kt   # 7 unit test
tools/gen_data.py                # script sinh lại queries.tsv
```

**Nên đọc theo thứ tự:** `Suggester.kt` → `LinearSuggester.kt` → `BasicTrie.kt` → `TopKTrie.kt`.
Ba file sau cùng triển khai cùng một interface, nên rất dễ so sánh với nhau.

---

## Thuật toán hoạt động thế nào

Cả 3 cách đều triển khai chung một interface:

```kotlin
interface Suggester {
    fun suggest(prefix: String, k: Int = 5): List<Suggestion>
}
```

### Cách 1 – Quét danh sách

Duyệt toàn bộ N truy vấn, lọc những truy vấn bắt đầu bằng tiền tố, sắp xếp theo tần suất rồi lấy 5.
Tương đương câu SQL trong sách. Đơn giản, nhưng **mỗi lần gõ phím đều quét toàn bộ dữ liệu**.

### Cách 2 – Trie cơ bản

```
            (gốc)
           /     \
          t       w
        /   \      \
      tr     to     wi
    /  |  \    \    /  \
 tree true try toy wish win
```

1. Đi từ gốc theo từng ký tự để tìm nút tiền tố: **O(p)**.
2. Duyệt toàn bộ cây con để gom các truy vấn: **O(c)**.
3. Sắp xếp và lấy top k: **O(c log c)**.

### Cách 3 – Trie lưu sẵn top-k ⭐

Hai tối ưu của sách:

1. **Giới hạn độ dài tiền tố** (`MAX_PREFIX = 50`), nên bước tìm nút thành **O(1)**.
2. **Mỗi nút lưu sẵn 5 gợi ý tốt nhất** của cây con bên dưới, nên bước lấy kết quả thành **O(1)**.

```kotlin
override fun suggest(prefix: String, k: Int): List<Suggestion> {
    val p = Normalizer.normalize(prefix)
    if (p.isEmpty() || p.length > MAX_PREFIX) return emptyList()
    var node = root
    for (ch in p) node = node.children[ch] ?: return emptyList()   // đi xuống nút tiền tố
    return node.topK.take(k)                                        // đọc danh sách có sẵn
}
```

**Mẹo khi xây cây:** chèn các truy vấn theo **tần suất giảm dần**. Khi đó k truy vấn đầu tiên đi qua một nút
chính là top-k của nút đó, nên không cần sắp xếp lại.

**Cái giá phải trả:** khi một truy vấn đổi tần suất, mọi nút tổ tiên đều phải cập nhật lại danh sách top-k
(`TopKTrie.upsert()`). Vì vậy sách chọn **xây lại cả cây theo lô mỗi tuần** thay vì cập nhật theo thời gian thực.

---

## Kỹ thuật phía app Android

Cài đặt trong [`SearchViewModel.kt`](app/src/main/java/com/example/sourcecode/ui/search/SearchViewModel.kt):

```kotlin
queryFlow
    .debounce { if (debounceOn) 300L else 0L }   // chỉ tra khi ngừng gõ 300 ms
    .distinctUntilChanged()
    .mapLatest { q -> lookup(q) }                // có chữ mới thì hủy lần tra cũ
    .collect { result -> /* cập nhật UI */ }
```

| Vấn đề | Giải pháp | Trong code | Cách thấy trong app |
|---|---|---|---|
| Mỗi phím gõ = 1 request | Debounce | `debounce(300)` | Tắt công tắc, gõ nhanh: *Số lần tra cứu* tăng theo từng phím |
| Kết quả cũ về muộn ghi đè kết quả mới | Hủy request cũ | `mapLatest` | Bật *mạng chậm*, gõ nhanh: *Request bị hủy* tăng |
| Gõ lại cùng tiền tố | Cache theo tiền tố (như cache trình duyệt của Google) | `LruCache(200)` | Xóa chữ rồi gõ lại: *Lấy từ cache* tăng |
| Gõ không dấu | Bỏ dấu làm khóa tra cứu, giữ chuỗi có dấu để hiển thị | `Normalizer` | Gõ `pho bo` ra `phở bò` |

---

## Dữ liệu

- **Ví dụ trong sách** (`QueryRepository.bookExamples`): `tree, try, true, toy, wish, win`, nhóm `be…`, nhóm `tw…`, `dinner…`
  và vài món ăn tiếng Việt.
- **Dữ liệu lớn** (`app/src/main/assets/queries.tsv`): **13.874 truy vấn** tự sinh, gồm món ăn và địa danh Việt Nam,
  sản phẩm, công nghệ, du lịch, học tập.
  - Tần suất theo **phân phối Zipf**: ít truy vấn rất phổ biến, đa số hiếm, giống dữ liệu tìm kiếm thật.
  - Sinh lại bằng: `python tools/gen_data.py app/src/main/assets/queries.tsv` (seed cố định, kết quả luôn giống nhau).

Đây là dữ liệu mẫu để minh họa, không phải dữ liệu tìm kiếm thật.

---

## Ánh xạ với nội dung sách

| Nội dung Chương 13 | Trong code |
|---|---|
| Thiết kế ban đầu: bảng tần suất + SQL `LIKE` | `LinearSuggester` |
| Trie cơ bản, O(p) + O(c) + O(c log c) | `BasicTrie` |
| Giới hạn tiền tố + lưu sẵn top-k → O(1) | `TopKTrie` |
| Cập nhật một nút kéo theo sửa nút tổ tiên (`beer` 10 → 30) | `TopKTrie.upsert()` + nút trong Tab 1 |
| Xây lại cây theo lô (Workers) | `TopKTrie` khởi tạo từ dữ liệu đã sắp xếp |
| Cache tại trình duyệt (`max-age=3600`) | `LruCache` trong `SearchViewModel` |
| Mở rộng đa ngôn ngữ (Unicode) | `Normalizer` + nút Trie dùng `HashMap<Char, Node>` |

**Chưa cài đặt** (phần kiến trúc máy chủ, nằm ngoài phạm vi một app): Load Balancer, Trie DB, Shard Map Manager,
xử lý luồng với Kafka. Các phần này được trình bày trong báo cáo và slide.

---

## Kịch bản demo 2 phút

1. **Tab Tìm kiếm**, dữ liệu sách: gõ `be` → `[best, bet, bee, be, beer]`, đúng như sách.
2. Bấm **cập nhật beer 10 → 30** → `beer` nhảy lên vị trí 2, app báo phải sửa 4 nút.
3. Chuyển **dữ liệu lớn**, gõ `pho bo` (không dấu) → ra gợi ý có dấu `phở bò …`.
4. **Tắt debounce**, gõ nhanh → số lần tra cứu tăng vọt; **bật lại** → chỉ còn 1–2 lần.
5. **Tab So sánh** → *Chạy so sánh* → chỉ vào cột thời gian (Trie top-k nhanh nhất) và cột lưu trữ (Trie top-k tốn nhất).

---

## Tác giả và tài liệu tham khảo

- **Sinh viên:** Hoàng Anh Kiệt – 077206008228
- **Môn học:** Lập trình thiết bị di động – bài giữa kỳ

**Tài liệu tham khảo:**

1. Alex Xu, *System Design Interview – An Insider's Guide, 2nd Edition*, Chương 13: Design a Search Autocomplete System.
2. Facebook Engineering, *The Life of a Typeahead Query*.
3. Android Developers, [Kotlin flows on Android](https://developer.android.com/kotlin/flow).
4. Android Developers, [Jetpack Compose](https://developer.android.com/compose).
