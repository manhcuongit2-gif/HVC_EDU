# Luyện Thi Hùng Cường

## Báo cáo buổi học và học phí theo điểm danh

Admin quản lý lịch tháng; các buổi được sinh từ lịch tuần trong khoảng ngày của lớp. Giáo viên phụ trách nhập báo cáo từng học sinh và công bố trong hệ thống. Phụ huynh xem báo cáo buổi/tháng tại `/parent/reports`.

Mỗi lớp cần có **đơn giá mỗi buổi** trong trang quản lý lớp để hệ thống tự tính học phí từ số buổi có mặt. Trường học phí trọn gói cũ vẫn được giữ cho các khoản thu thủ công.

### Cập nhật cơ sở dữ liệu hiện có

Trước khi chạy phiên bản mới trên DB đã có dữ liệu, chạy một lần các script cộng dồn trong `database/migration/` theo thứ tự phiên bản trên `educenter_db`, sau đó cấu hình đơn giá mỗi buổi cho các lớp. Các script này không xóa dữ liệu.

`csdl.sql` dành cho cài đặt DB mới và có lệnh xóa/tạo lại bảng; không chạy script đó trên DB hiện có cần giữ dữ liệu.
