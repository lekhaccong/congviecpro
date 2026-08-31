# Cơ chế lịch sử thay đổi trong app

## Nhật ký runtime
Mỗi thao tác nghiệp vụ đi qua `AppViewModel.commit()` và tạo `HistoryEntry` gồm:
- `time`: thời điểm thay đổi
- `action`: THÊM / SỬA / XÓA / THÊM ẢNH / IMPORT / RESET
- `module`: module bị thay đổi
- `detail`: đối tượng/nội dung thay đổi

Tối đa 500 sự kiện gần nhất được giữ lại.

## Nhật ký source code
File `CHANGELOG.md` ghi lịch sử phiên bản code.

## Nguyên tắc khi nâng cấp
1. Không xóa `state_json` nếu chưa có migration.
2. Field JSON mới luôn dùng `optString/optInt/...` với mặc định.
3. Mọi nút nghiệp vụ phải gọi ViewModel, không sửa state trong Composable.
4. Mọi thêm/sửa/xóa phải ghi HistoryEntry.
5. Không ghi mật khẩu hoặc dữ liệu nhạy cảm vào history.
