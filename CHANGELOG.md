# CHANGELOG - CongViecPro

## 2.0.0 — 31/08/2026
**Mục tiêu:** app có dữ liệu thật + GitHub Actions build được APK.

### Thêm
- Lưu dữ liệu cục bộ JSON/SharedPreferences.
- CRUD nhân sự, check-in.
- CRUD khối công việc, tiến độ, ảnh.
- OT/AMH và thống kê giờ.
- DATA/Hàng xuất/Lot: thêm, đổi trạng thái, chốt Lot, xóa, ảnh.
- Checklist 3S/3D.
- Mail chốt Lot / báo bất thường.
- Báo cáo KPI + nhật ký 500 sự kiện.
- Backup ZIP/JSON.
- FileProvider + quyền Camera.
- GitHub Actions: JDK 17, Android SDK 36, Gradle 8.13, artifact APK.
- NavigationBar cho điện thoại.

### Sửa
- Bật Compose (`buildFeatures.compose = true`) — thiếu thì không compile.
- `ItemDialog` dùng `values[0]…[3]` thay vì `.first/.second/.third/.fourth` trên Array.
- Workflow gốc thiếu Android SDK — assembleDebug sẽ fail trên runner.
- Thiếu launcher icon / strings.
- `reset()` bỏ qua `repo.load()` và tạo `AppState()` rỗng.

### Thay đổi
- versionCode: 2
- versionName: 2.0.0
