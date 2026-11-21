# Hướng dẫn sử dụng Hùng Cường Exam

## Cơ sở dữ liệu và tệp

Flyway tự áp dụng migration bổ sung `V20261204__pdf_and_direct_exams.sql` khi ứng dụng khởi động thành công. Migration giữ các đề ma trận, lượt thi và kết quả hiện có; đề cũ được xem là loại `ONLINE`. Không cần chạy lệnh SQL thủ công.

Tệp đề PDF và ảnh bài làm được lưu ngoài `static`, trong `exams/` dưới `app.storage.upload-dir` (mặc định `./uploads`). Giới hạn giữ theo cấu hình hiện tại: 20 MB mỗi tệp và 22 MB mỗi yêu cầu; một lần nộp ảnh có tổng dung lượng tối đa 20 MB.

## Giáo viên: tạo đề PDF

1. Mở **Đề thi → Tạo đề thi → Đề PDF**.
2. Chọn PDF, chọn lớp, nhập hạn làm bài. Có thể bỏ trống tên đề để dùng tên tệp.
3. Mở trang đề đã giao để xem PDF và các phiên bản bài nộp theo học sinh.
4. Với bài ở trạng thái **Chờ chấm**, mở từng ảnh, nhập điểm từ 0 đến 10 và nhận xét rồi lưu. Kết quả hiện ngay cho học sinh và phụ huynh đã liên kết.

Học sinh được nộp lại trước hạn khi chưa có phiên bản nào được chấm. Các phiên bản cũ được lưu, đánh dấu đã thay thế và vẫn xem được trong lịch sử. Khi một phiên bản đã được chấm, học sinh không thể nộp lại.

## Giáo viên: tạo đề nhập trực tiếp

1. Mở **Đề thi → Tạo đề thi → Đề nhập trực tiếp**.
2. Nhập tên đề, lớp, thời gian, thời lượng và số lượt. Dùng **Thêm câu hỏi** để tạo thêm câu.
3. Chọn dạng câu: trắc nghiệm, đúng/sai, trả lời ngắn hoặc tự luận. Mỗi câu cần nội dung và điểm; trắc nghiệm cần ít nhất hai lựa chọn và đáp án đúng, trả lời ngắn cần ít nhất một đáp án được chấp nhận.
4. Tạo và giao đề. Câu trắc nghiệm/đúng-sai được chấm tự động; trả lời ngắn so khớp sau khi chuẩn hóa khoảng trắng và không phân biệt chữ hoa/thường. Câu tự luận chờ giáo viên chấm. Kết quả trực tuyến chỉ được công bố theo quy trình hiện có sau khi đề đóng và các câu tự luận đã chấm xong.

## Học sinh

Tài khoản học sinh phải được Admin tạo hoặc liên kết với đúng hồ sơ. Trong **Bài thi**, học sinh chỉ thấy đề của lớp mình đang học.

- Với đề PDF, mở đề, chọn một hoặc nhiều ảnh JPG/JPEG/PNG, kiểm tra phần xem trước rồi nộp. Trạng thái sẽ là **Chờ chấm**; nếu chưa được chấm, học sinh có thể nộp lại trước hạn.
- Với đề trực tuyến, bắt đầu hoặc tiếp tục làm bài. Câu trả lời được lưu tự động. Điểm chỉ hiện sau khi giáo viên công bố kết quả.

## Phụ huynh và điểm chính thức

Phụ huynh chỉ xem kết quả của học sinh liên kết với tài khoản. Điểm đề PDF hiển thị sau khi giáo viên chấm; điểm trực tuyến tuân theo thời điểm công bố hiện có. Điểm chính thức của các lượt trực tuyến vẫn là điểm cao nhất trong các lượt đã chấm. Điểm thi không thay đổi dữ liệu điểm môn học.

Các đề ma trận đã giao trước migration vẫn làm, chấm và công bố theo luồng cũ. Tạo đề mới từ màn hình chính hiện có hai lựa chọn: PDF và nhập trực tiếp.
