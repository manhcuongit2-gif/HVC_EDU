# Luyện Thi Hùng Cường

## Báo cáo buổi học và học phí theo điểm danh

Admin quản lý lịch tháng; các buổi được sinh từ lịch tuần trong khoảng ngày của lớp. Giáo viên phụ trách nhập báo cáo từng học sinh và công bố trong hệ thống. Phụ huynh xem kết quả điểm danh và báo cáo học tập tại `/parent/results`.

Mỗi lớp cần có **đơn giá mỗi buổi** trong trang quản lý lớp để hệ thống tự tính học phí từ số buổi có mặt. Trường học phí trọn gói cũ vẫn được giữ cho các khoản thu thủ công.

### Cập nhật cơ sở dữ liệu hiện có

Trên database hiện có, các script `V20261117` và `V20261118` trong `database/migration/` vẫn cần được chạy theo thứ tự nếu trước đó chưa áp dụng. Flyway tự chạy migration `V20261119` khi ứng dụng khởi động lần đầu; migration này tạo các bảng tài liệu và hội thoại còn thiếu, không xóa dữ liệu. Flyway baseline schema hiện có ở phiên bản `0`; không bật Hibernate tự sửa schema.

Với database mới, chạy `csdl.sql` và `data.sql` trước khi khởi động ứng dụng. `csdl.sql` dùng `CREATE TABLE IF NOT EXISTS` và không xóa các bảng hiện có.
