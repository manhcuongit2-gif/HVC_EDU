# Luyện Thi Hùng Cường

## Báo cáo buổi học và học phí theo điểm danh

Admin quản lý lịch tháng; các buổi được sinh từ lịch tuần trong khoảng ngày của lớp. Giáo viên phụ trách nhập báo cáo từng học sinh và công bố trong hệ thống. Phụ huynh xem kết quả điểm danh và báo cáo học tập tại `/parent/results`.

Mỗi lớp cần có **đơn giá mỗi buổi** trong trang quản lý lớp để hệ thống tự tính học phí từ số buổi có mặt. Trường học phí trọn gói cũ vẫn được giữ cho các khoản thu thủ công.

### Cập nhật cơ sở dữ liệu hiện có

Trên database hiện có, các script `V20261117` và `V20261118` trong `database/migration/` vẫn cần được chạy theo thứ tự nếu trước đó chưa áp dụng. Flyway tự chạy migration `V20261119` khi ứng dụng khởi động lần đầu; migration này tạo các bảng tài liệu và hội thoại còn thiếu, không xóa dữ liệu. Flyway baseline schema hiện có ở phiên bản `0`; không bật Hibernate tự sửa schema.

Với database mới, chạy `csdl.sql` và `data.sql` trước khi khởi động ứng dụng. `csdl.sql` dùng `CREATE TABLE IF NOT EXISTS` và không xóa các bảng hiện có.

## Hùng Cường AI

Hùng Cường AI dùng Groq Chat Completions. Cấu hình `GROQ_API_KEY` trong môi trường của máy chạy ứng dụng; không đặt khóa trong mã nguồn hoặc trình duyệt. Có thể ghi đè `GROQ_BASE_URL`, `GROQ_MODEL`, `GROQ_ALLOWED_MODELS` và tắt toàn bộ tích hợp bằng `AI_ENABLED=false`. Khởi động lại ứng dụng sau khi thay đổi biến môi trường.

Flyway tự tạo các bảng AI từ migration `V20261129`. Tích hợp và các vai trò đều **tắt mặc định**. Admin vào `/admin/ai-management`, đặt giới hạn yêu cầu/token, chọn mô hình Groq trong danh sách cho phép, rồi bật từng vai trò cần dùng.

Hệ thống không tự chuyển sang dịch vụ trả phí. Người dạy có thể trích xuất văn bản từ PDF, Word, Excel và TXT; PDF scan không có OCR. Nhận xét, bài tập và đề thi AI tạo là bản nháp để người dạy kiểm tra, không tự lưu hoặc công bố. Các truy vấn hồ sơ phụ huynh (học phí, điểm, lịch và bài tập của con) được tính và trình bày trong ứng dụng, không gửi dữ liệu kết quả đó đến Groq. Truy vấn của người dạy/quản trị dùng phạm vi được phân quyền và ẩn danh tên học sinh trước khi gửi dữ liệu cần thiết đến mô hình.
