# Hướng dẫn phân hệ học sinh

## Áp dụng migration

Sao lưu MySQL trước khi triển khai. Ứng dụng tự chạy Flyway khi khởi động; migration mới là `V20261130__student_portal_and_multi_account_push.sql`. Không chạy lại SQL thủ công sau khi Flyway đã ghi nhận phiên bản.

Migration thêm vai trò `ROLE_STUDENT`, liên kết tùy chọn `students.user_id`, câu trả lời văn bản cho bài nộp và các liên kết push riêng theo tài khoản. Các bài nộp tệp cũ được giữ nguyên. Subscription cũ được chuyển thành liên kết cho đúng tài khoản đang sở hữu; không cần tạo lại subscription.

## Tạo hoặc liên kết tài khoản học sinh

1. Đăng nhập bằng Admin, mở **Quản lý hồ sơ học sinh** và chọn **Quản lý tài khoản** trên đúng hồ sơ.
2. Để tạo mới, nhập username, email chưa được dùng và mật khẩu ban đầu. Mật khẩu được mã hóa trong cơ sở dữ liệu; Admin giao mật khẩu riêng cho học sinh hoặc phụ huynh. Không dùng mật khẩu mặc định.
3. Để liên kết tài khoản đã có, chọn tài khoản `ROLE_STUDENT` đang hoạt động và chưa liên kết hồ sơ. Tài khoản Admin, Giáo viên hoặc Phụ huynh không thể gắn vào hồ sơ học sinh.
4. Học sinh đăng nhập bằng username tại `/login`; sau đăng nhập ứng dụng chuyển đến `/student/dashboard`. Đăng ký công khai hiện có vẫn dành cho Phụ huynh.

## Nguồn dữ liệu tiến độ

- Lớp và lịch lấy từ `class_students` và `class_sessions` đã đồng bộ từ lịch lớp; lịch chỉ gồm buổi sắp tới chưa bị hủy.
- Điểm lấy trực tiếp từ các trường đã lưu trong `grades`; `average_score` không được tính lại trong cổng học sinh.
- Chuyên cần đếm trạng thái PRESENT, ABSENT và EXCUSED từ báo cáo buổi đã công bố (`session_student_reports`); với lớp/ngày chưa có báo cáo buổi công bố thì dùng bản ghi cũ trong `attendance`. Như vậy cùng một buổi không bị đếm hai lần; buổi chưa có bản ghi không nằm trong số đếm.
- Bài tập lấy từ `homework_assignments` của lớp đang học. Trạng thái chưa nộp/quá hạn so sánh `due_at` với thời điểm hiện tại; trạng thái đã nộp/đã chấm và nộp muộn lấy từ `homework_submissions`.
- Nhận xét chỉ đọc báo cáo buổi có `published_at` và báo cáo tháng có `published_at`; điểm bài tập/điểm trung bình trong báo cáo dùng giá trị đã lưu.

## Kiểm tra các luồng chính

1. Tạo hai hồ sơ học sinh thuộc các phụ huynh khác nhau, ghi danh hai lớp và liên kết mỗi hồ sơ với một tài khoản Student. Đăng nhập từng tài khoản, kiểm tra lớp, lịch, bài tập, điểm, điểm danh và báo cáo chỉ khớp hồ sơ đó.
2. Thử mở đường dẫn `/student/homework/submission/{id}/file` của học sinh khác, nộp bài vào lớp không ghi danh, và gọi `/student/**` bằng tài khoản Parent/Teacher/Admin. Các yêu cầu phải bị từ chối.
3. Giáo viên tạo bài tập có hạn nộp và tệp hướng dẫn. Học sinh mở bài, nộp câu trả lời, nộp tệp, sau đó nộp lại; giáo viên mở bài nộp, tải tệp nếu có, chấm điểm và ghi nhận xét. Kiểm tra Phụ huynh vẫn xem/nộp tệp thay con theo luồng cũ.
4. So sánh trang tiến độ với `grades`, `attendance`, `homework_submissions`, `session_student_reports` và `monthly_student_reports`; xác nhận các điểm và nhận xét khớp giá trị đã lưu.
5. Trên cùng trình duyệt bật push riêng cho tài khoản Admin, Teacher và Parent tại **Thông báo → Quản lý thiết bị**. Tạo sự kiện nhận biết được cho từng tài khoản, tắt liên kết của một tài khoản và xác nhận hai tài khoản còn lại vẫn nhận. Lặp lại với Student. Dùng HTTPS và VAPID đã cấu hình để kiểm tra push thật; payload màn hình khóa chỉ hiển thị nội dung chung.

Chạy `mvn test` để kiểm tra tự động. Kiểm tra push ngoài trình duyệt cần VAPID và quyền thông báo; trên iPhone/iPad cần cài trang vào Màn hình chính trước.
