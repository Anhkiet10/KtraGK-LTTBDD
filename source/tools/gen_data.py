"""Sinh dữ liệu mẫu (~14.000 truy vấn tiếng Việt + tiếng Anh) với tần suất theo phân phối Zipf."""
# Cách dùng: python tools/gen_data.py app/src/main/assets/queries.tsv
import random, sys

random.seed(2026)
dishes = ["phở bò", "phở gà", "bún chả", "bún bò Huế", "bánh mì", "bánh xèo", "cơm tấm", "gỏi cuốn",
          "cà phê sữa đá", "trà sữa", "bánh cuốn", "hủ tiếu", "mì Quảng", "cao lầu", "chả cá", "lẩu thái",
          "lẩu mắm", "bò kho", "bún riêu", "bánh canh", "xôi xéo", "chè thái", "nem rán", "cháo lòng",
          "bánh tráng trộn", "ốc len xào dừa", "gà rán", "pizza", "hamburger", "sushi", "mì cay", "bò bít tết"]
places = ["Hà Nội", "Sài Gòn", "Đà Nẵng", "Huế", "Hội An", "Nam Định", "Cần Thơ", "Đà Lạt", "Hải Phòng",
          "Nha Trang", "Vũng Tàu", "Quận 1", "Quận 3", "Thủ Đức", "Bình Thạnh", "Gò Vấp", "Hoàn Kiếm", "Cầu Giấy"]
food_mods = ["ngon", "gần đây", "giá rẻ", "nổi tiếng", "mở cửa khuya", "giao tận nơi", "chay", "đặc biệt"]
food_pre = ["cách nấu", "cách làm", "quán", "công thức", "review", "giá"]
products = ["iphone 17", "iphone 16", "samsung galaxy s26", "laptop gaming", "macbook air", "tai nghe bluetooth",
            "đồng hồ thông minh", "bàn phím cơ", "chuột không dây", "màn hình 27 inch", "máy lọc không khí",
            "nồi chiên không dầu", "quạt điều hòa", "giày thể thao", "áo khoác nam", "váy công sở", "balo laptop",
            "sạc dự phòng", "loa bluetooth", "máy ảnh mirrorless", "xe máy điện", "ghế công thái học"]
prod_mods = ["giá rẻ", "chính hãng", "trả góp", "cũ", "mới nhất", "so sánh", "review", "khuyến mãi", "màu đen", "2026"]
tech = ["kotlin", "jetpack compose", "android studio", "flutter", "java", "python", "react", "swift",
        "firebase", "room database", "retrofit", "coroutines", "lazycolumn", "navigation compose", "viewmodel",
        "system design", "trie", "binary search", "linked list", "hash map", "docker", "git", "sql", "kafka"]
tech_mods = ["tutorial", "là gì", "cơ bản", "ví dụ", "cho người mới", "nâng cao", "interview", "tiếng việt",
             "documentation", "best practices", "vs", "cài đặt", "lỗi thường gặp"]
english = ["weather", "weather tomorrow", "news", "dinner ideas", "dinner recipes", "dinner near me", "dinosaur",
           "translate", "twitter", "twitch", "twilight", "twin peaks", "youtube", "youtube music", "facebook",
           "gmail", "google maps", "netflix", "spotify", "amazon", "ebay", "wikipedia", "world cup", "world map",
           "windows 11", "wifi password", "word counter", "zalo", "shopee", "tiki", "lazada", "grab", "be taxi"]
travel = ["vé máy bay", "khách sạn", "homestay", "tour du lịch", "thời tiết", "lịch trình", "địa điểm check in",
          "bản đồ", "xe khách", "tàu hỏa"]
study = ["điểm chuẩn", "học bổng", "lịch thi", "đề thi", "tài liệu", "giáo trình", "bài tập", "đáp án"]
subjects = ["lập trình thiết bị di động", "cấu trúc dữ liệu", "giải thuật", "cơ sở dữ liệu", "mạng máy tính",
            "toán rời rạc", "tiếng anh", "kinh tế vĩ mô", "xác suất thống kê", "trí tuệ nhân tạo"]
years = ["2024", "2025", "2026"]

q = set()
for d in dishes:
    q.add(d)
    for p in places: q.add(f"{d} {p}")
    for m in food_mods: q.add(f"{d} {m}")
    for pre in food_pre: q.add(f"{pre} {d}")
    for p in places:
        for m in food_mods: q.add(f"{d} {m} {p}")
        for pre in food_pre: q.add(f"{pre} {d} {p}")
for pr in products:
    q.add(pr)
    for m in prod_mods: q.add(f"{pr} {m}")
    for p in places: q.add(f"mua {pr} {p}")
    for m in prod_mods:
        for m2 in prod_mods:
            if m != m2: q.add(f"{pr} {m} {m2}")
for t in tech:
    q.add(t)
    for m in tech_mods: q.add(f"{t} {m}")
    for t2 in random.sample(tech, 5):
        if t2 != t: q.add(f"{t} vs {t2}")
for e in english: q.add(e)
for t in travel:
    for p in places:
        q.add(f"{t} {p}")
        for y in years: q.add(f"{t} {p} {y}")
for s in study:
    for sub in subjects:
        q.add(f"{s} {sub}")
        for y in years: q.add(f"{s} {sub} {y}")
for p in places:
    for p2 in places:
        if p != p2: q.add(f"xe {p} đi {p2}")
        if p != p2: q.add(f"khoảng cách {p} {p2}")

q = sorted(q)
random.shuffle(q)
# Tần suất theo Zipf: một số ít truy vấn rất phổ biến, đa số hiếm
lines = []
for rank, text in enumerate(q, start=1):
    freq = int(2_000_000 / (rank ** 0.9)) + random.randint(1, 50)
    lines.append(f"{text}\t{freq}")
out = sys.argv[1]
with open(out, "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")
print(len(lines), "queries ->", out)
