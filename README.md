# congviecpro

Ứng dụng Android Kotlin + Jetpack Compose, kiến trúc module nghiệp vụ theo:
Dashboard / Nhân sự / Công việc / OT-AMH / Hàng / 3S-3D / Mail / Báo cáo / Backup.

## Bản 1.0
- Có giao diện và luồng màn hình chính.
- Dashboard tổng quan.
- Nhân sự, chia nhóm, ca.
- Khối công việc và tiến độ.
- OT/AMH.
- DATA, hàng xuất, Lot, QR UI.
- 3S/3D checklist và nút camera.
- Mail.
- Báo cáo.
- Backup/Import/Export UI.

## Nâng cấp tiếp theo
1. Room database + migration.
2. CameraX lưu ảnh vào MediaStore/app storage.
3. QR/Barcode bằng ML Kit.
4. Backup ZIP/JSON + ảnh, checksum và version.
5. Chấm công/ca/OT tự động.
6. Hàng xuất/DATA/Lot theo mã thực tế.
7. Email Intent.
8. Notification/WorkManager.
9. Phân quyền người dùng.

## GitHub Actions

Workflow: `.github/workflows/build-apk.yml`

Mỗi lần push code, GitHub Actions sẽ build:
`app-debug.apk`

APK được lưu trong Actions Artifacts với tên:
`congviecpro-debug-apk`
