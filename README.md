# CongViecPro 2.0.0

Ứng dụng Android quản lý công việc nhà máy: Dashboard, Nhân sự, Công việc, OT/AMH, Hàng–DATA–Lot, 3S/3D, Mail, Báo cáo, Backup.

Bản này đã được xử lý để **build APK trên GitHub Actions** (source gốc 2.0.0 thiếu SDK, Compose flag, icon, và có lỗi Kotlin không compile).

## Build APK trên GitHub (không cần Android Studio)

1. Tạo repository trống trên GitHub (ví dụ `congviecpro`).
2. Upload toàn bộ file trong thư mục này lên nhánh `main` (kéo-thả trên web cũng được).
3. Vào tab **Actions** → workflow **Build Android APK** sẽ chạy tự động.
4. Khi job thành công, tải artifact `CongViecPro-2.0.0-debug` → file `app-debug.apk`.
5. Cài APK trên điện thoại (cho phép cài từ nguồn không xác định).

Có thể bấm **Run workflow** bất kỳ lúc nào (nút `workflow_dispatch`).

## Chức năng

- Dữ liệu lưu cục bộ SharedPreferences + JSON, không mất khi thoát app.
- Nhân sự: thêm, sửa, xóa, nhóm, ca, check-in.
- Công việc: khối việc, tiến độ +1, ghi chú, chụp ảnh.
- OT/AMH: khai báo người, ngày, giờ, loại; tổng hợp và xóa.
- Hàng: DATA / Hàng xuất / Lot; đổi trạng thái, chốt Lot, ảnh.
- 3S/3D: checklist + ảnh.
- Mail: mở email soạn chốt Lot / báo bất thường.
- Báo cáo KPI từ dữ liệu thật + nhật ký tối đa 500 sự kiện.
- Backup ZIP (JSON + ảnh) / JSON.

## Camera

Ảnh lưu `files/photos` qua FileProvider. URI được ghi cùng dữ liệu.

## Kỹ thuật

| | |
|---|---|
| applicationId | `com.congviechangngay.pro` |
| version | 2.0.0 (versionCode 2) |
| minSdk / compileSdk | 26 / 36 |
| UI | Jetpack Compose Material3 |
| JDK | 17 |
| Gradle | 8.13 |
| AGP / Kotlin | 8.11.1 / 2.2.10 |

Workflow: `.github/workflows/build-apk.yml`

## Đã sửa so với source gốc

- Bật `buildFeatures { compose = true }`.
- Sửa `ItemDialog` (`Array[0]…[3]` thay vì `.first/.second` không tồn tại).
- Thêm Android SDK + license vào GitHub Actions.
- Thêm adaptive icon, strings, colors.
- `reset()` trả về dữ liệu mẫu thay vì state rỗng.
- NavigationBar trên điện thoại (`< 600dp`), NavigationRail trên máy tính bảng.
- Tách `Data.kt` / `MainActivity.kt` cho dễ bảo trì.

## Build local (tuỳ chọn)

Cần JDK 17 + Android SDK. GitHub Actions tự tạo Gradle Wrapper:

```
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`
