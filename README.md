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

## Giai đoạn 4 – AI gia sư và cài đặt tài khoản

Flyway tự áp dụng migration bổ sung `V20261205__student_ai_personalization_and_account_settings.sql` khi ứng dụng khởi động. Migration này thêm cờ bật AI cho học sinh, đường dẫn ảnh đại diện riêng, trạng thái lập chỉ mục tài liệu và các bảng chỉ mục/ kế hoạch học tập; không xóa hoặc thay đổi dữ liệu điểm hiện có. Không cần chạy migration thủ công nếu Flyway đã hoạt động.

AI học sinh dùng chung Groq, quota, hội thoại và `AIDocumentService` hiện có. Trên máy chạy ứng dụng, cấu hình ví dụ sau trong môi trường rồi khởi động lại:

```text
GROQ_API_KEY=<Groq API key>
GROQ_BASE_URL=https://api.groq.com/openai/v1
GROQ_MODEL=openai/gpt-oss-120b
GROQ_ALLOWED_MODELS=openai/gpt-oss-120b,qwen/qwen3.8-27b
AI_ENABLED=true
EDUCENTER_UPLOAD_DIR=./uploads
```

Không đưa khóa thật vào Git. `GROQ_BASE_URL`, `GROQ_MODEL`, `GROQ_ALLOWED_MODELS` và `AI_ENABLED` đã được hỗ trợ qua cấu hình ứng dụng. Admin vào `/admin/ai-management`, bật Hùng Cường AI, chọn model trong danh sách cho phép, bật quyền **Học sinh** và đặt hạn mức. Nếu chọn một danh sách model tùy chỉnh không có `qwen/qwen3.8-27b`, học sinh vẫn có thể trò chuyện văn bản nhưng không thể gửi ảnh.

Học sinh mở **Học cùng AI** hoặc `/student/ai`. Cuộc trò chuyện được lưu theo tài khoản sở hữu; endpoint kiểm tra `ROLE_STUDENT`, hồ sơ học sinh đang liên kết và quyền sở hữu hội thoại. Ảnh chỉ nhận JPG/JPEG/PNG, tối đa theo `ai.groq.max-upload-bytes` (mặc định 10 MB), được kiểm tra nội dung ảnh và gửi trực tiếp trong yêu cầu hiện tại, không lưu thành tệp. Model văn bản không nhận ảnh. Không có OCR; PDF scan và hình ảnh nằm bên trong tài liệu lớp không được nhận dạng.

RAG chỉ đọc tệp `LearningMaterial` của các lớp học sinh đang theo học, với định dạng PDF, DOC, DOCX, XLS, XLSX và TXT mà `AIDocumentService` hỗ trợ. Dung lượng một tài liệu để trích xuất bị giới hạn bởi cùng `ai.groq.max-upload-bytes` (mặc định 10 MB), dù luồng tải tài liệu hiện tại có thể nhận tệp lớn hơn. Tìm kiếm hiện dùng khớp từ khóa cục bộ trên các đoạn văn bản, không dùng embedding/vector database; trích đoạn phù hợp hiển thị tên tài liệu và trang hoặc sheet nếu có. Tài liệu lỗi, không có lớp văn bản hoặc vượt giới hạn bị bỏ qua thay vì giả làm nguồn đã đọc.

Phân tích chủ đề chỉ dùng cờ đúng/sai của câu trả lời trong đề thi trực tuyến đã công bố, đối chiếu topic đã lưu trong snapshot đề. Mỗi đề chỉ lấy lượt có điểm cao nhất đã chấm; không tính lại điểm chính thức và không dùng đề PDF để suy đoán chủ đề. Chỉ đưa nhận định khi có ít nhất 5 câu đã chấm thuộc chủ đề từ ít nhất 2 đề đã công bố; mỗi dòng hiển thị số câu đúng/tổng số câu và số đề. Kế hoạch luyện tập là nội dung gợi ý, không tự giao bài hay ghi điểm; trạng thái từng mục do học sinh cập nhật. Kế hoạch được làm mới một lần khi mở trang tiến độ nếu dấu vân tay dữ liệu đã lưu thay đổi, và giữ trạng thái các mục còn trùng chủ đề.

Tài khoản dùng chung tại `/account/settings` cho Admin, Giáo viên, Phụ huynh và Học sinh. Người dùng chỉ sửa thông tin của chính mình; họ tên và điện thoại của tài khoản học sinh được đồng bộ vào hồ sơ học sinh. Ảnh JPG/JPEG/PNG tối đa 5 MB được chuẩn hóa thành PNG trong thư mục riêng `avatars` dưới `EDUCENTER_UPLOAD_DIR`, ngoài `static`. Đổi mật khẩu cần mật khẩu hiện tại và đăng xuất phiên hiện tại sau khi lưu thành công.

Chạy dự án theo hướng dẫn hiện có bằng `mvn spring-boot:run` (hoặc `mvn test` để chạy bộ kiểm tra). Nếu chưa có khóa Groq hoặc Admin chưa bật AI, cổng học sinh và trang tiến độ vẫn dùng được; khu vực AI hiển thị trạng thái chưa sẵn sàng.
