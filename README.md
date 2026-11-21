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

## PWA và thông báo đẩy

Flyway áp dụng migration `V20261125__pwa_push_notifications_and_homework.sql` khi ứng dụng khởi động. Migration này tạo các bảng thông báo, thiết bị và hàng đợi gửi; không xóa bảng. Không chạy migration thủ công nếu Flyway đã ghi nhận phiên bản này.

Để bật Web Push, cấu hình các biến môi trường trên máy chủ rồi khởi động lại ứng dụng:

```text
APP_PUSH_ENABLED=true
APP_PUSH_VAPID_PUBLIC_KEY=<VAPID public key>
APP_PUSH_VAPID_PRIVATE_KEY=<VAPID private key>
APP_PUSH_VAPID_SUBJECT=mailto:admin@example.com
```

Tạo một cặp VAPID cho máy chủ, giữ khóa riêng trong secret store hoặc biến môi trường và không commit khóa vào Git. Mỗi tài khoản bật thông báo riêng tại **Thông báo → Quản lý thiết bị**. Cùng một subscription của trình duyệt có thể được nhiều tài khoản liên kết sau khi từng tài khoản chủ động bật; gỡ một liên kết không ảnh hưởng tài khoản còn lại. Khi push provider trả về `404` hoặc `410`, thiết bị hết hạn sẽ được vô hiệu hóa.

Để cài ứng dụng trên điện thoại, mở website bằng HTTPS, đăng nhập và dùng mục cài ứng dụng trên trình duyệt. Trên iPhone/iPad, thêm trang vào Màn hình chính bằng Safari rồi mở biểu tượng vừa tạo. Chỉ shell ngoại tuyến và tài nguyên tĩnh của ứng dụng được cache; trang đăng nhập, API và dữ liệu sau đăng nhập không được đưa vào cache service worker.


## Phân hệ học sinh

Flyway áp dụng migration `V20261130__student_portal_and_multi_account_push.sql` khi ứng dụng khởi động. Hướng dẫn tạo/liên kết tài khoản, nguồn dữ liệu tiến độ và kiểm tra luồng nằm tại [docs/STUDENT-PORTAL-SETUP.md](docs/STUDENT-PORTAL-SETUP.md).
