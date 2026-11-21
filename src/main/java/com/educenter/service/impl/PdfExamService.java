package com.educenter.service.impl;

import com.educenter.dto.PdfExamParentRow;
import com.educenter.dto.PdfExamStudentRow;
import com.educenter.dto.PdfExamTeacherRow;
import com.educenter.dto.SecureExamFile;
import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PdfExamService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final BigDecimal MAX_SCORE = BigDecimal.TEN;
    private static final String PENDING = "PENDING";
    private static final String SUPERSEDED = "SUPERSEDED";
    private static final String GRADED = "GRADED";
    private final ExamAssignmentRepository assignments;
    private final ExamPdfSubmissionRepository submissions;
    private final ExamPdfSubmissionFileRepository files;
    private final ClassRepository classes;
    private final ClassStudentRepository enrollments;
    private final StudentRepository students;
    private final UserRepository users;
    private final TeacherRepository teachers;
    private final NotificationService notifications;
    private final Path storage;

    public PdfExamService(ExamAssignmentRepository assignments, ExamPdfSubmissionRepository submissions,
                          ExamPdfSubmissionFileRepository files, ClassRepository classes,
                          ClassStudentRepository enrollments, StudentRepository students, UserRepository users,
                          TeacherRepository teachers, NotificationService notifications,
                          @Value("${app.storage.upload-dir:./uploads}") String uploadDirectory) {
        this.assignments = assignments; this.submissions = submissions; this.files = files; this.classes = classes;
        this.enrollments = enrollments; this.students = students; this.users = users; this.teachers = teachers;
        this.notifications = notifications;
        this.storage = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("exams").normalize();
    }

    @Transactional
    public ExamAssignment createForTeacher(Long teacherUserId, Long classId, String title, LocalDateTime closesAt, MultipartFile pdf) {
        User teacherUser = users.findById(teacherUserId).filter(u -> u.getRole() != null
                        && "ROLE_TEACHER".equals(u.getRole().getName()) && Boolean.TRUE.equals(u.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản giáo viên không hoạt động."));
        teachers.findByUserId(teacherUserId).orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có hồ sơ giáo viên."));
        ClassEntity classEntity = requireTeacherClass(teacherUserId, classId);
        if (closesAt == null || !closesAt.isAfter(now())) throw new IllegalArgumentException("Hạn làm bài phải ở tương lai.");
        ValidatedUpload upload = validatePdf(pdf);
        String examTitle = title == null || title.isBlank() ? titleFromFilename(upload.originalFilename()) : title.trim();
        if (examTitle.isBlank() || examTitle.length() > 150) throw new IllegalArgumentException("Tên đề bắt buộc, tối đa 150 ký tự.");
        String stored = UUID.randomUUID() + ".pdf";
        Path storedPath = store(stored, upload.bytes());
        try {
            ExamAssignment assignment = new ExamAssignment();
            assignment.setBlueprint(null); assignment.setClassEntity(classEntity); assignment.setCreatedBy(teacherUser);
            assignment.setTitle(examTitle); assignment.setExamType("PDF"); assignment.setPdfStoredFilename(stored);
            assignment.setPdfOriginalFilename(upload.originalFilename()); assignment.setPdfContentType("application/pdf");
            assignment.setPdfFileSize((long) upload.bytes().length); assignment.setScope(null);
            assignment.setOpensAt(now()); assignment.setClosesAt(closesAt); assignment.setDurationMinutes(0);
            assignment.setMaxAttempts(1); assignment.setShuffleQuestions(false); assignment.setShuffleOptions(false);
            assignment.setTotalPoints(MAX_SCORE); assignment.setQuestionsSnapshotJson("[]");
            ExamAssignment saved = assignments.save(assignment);
            notifyClass(saved, "exam-assigned-" + saved.getId(), "Có đề thi mới được giao. Hãy đăng nhập cổng học sinh để xem thời hạn làm bài.");
            return saved;
        } catch (RuntimeException e) {
            deleteQuietly(storedPath);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<PdfExamStudentRow> forStudent(Long studentUserId) {
        Student student = studentForUser(studentUserId);
        Set<Long> classIds = enrollments.findByStudentId(student.getId()).stream().filter(this::currentEnrollment)
                .map(e -> e.getClassEntity().getId()).collect(Collectors.toSet());
        if (classIds.isEmpty()) return List.of();
        LocalDateTime current = now();
        List<ExamAssignment> found = classIds.stream().flatMap(id -> assignments.findByClassEntityIdOrderByOpensAtDesc(id).stream())
                .filter(ExamAssignment::isPdfExam).distinct().toList();
        return found.stream().map(assignment -> {
            List<ExamPdfSubmission> history = submissions.findByAssignmentIdAndStudentIdOrderByVersionNumberDesc(assignment.getId(), student.getId());
            ExamPdfSubmission latest = history.isEmpty() ? null : history.get(0);
            String status;
            if (latest != null && GRADED.equals(latest.getStatus())) status = GRADED;
            else if (latest != null && PENDING.equals(latest.getStatus())) status = PENDING;
            else if (current.isBefore(assignment.getOpensAt())) status = "UPCOMING";
            else if (!current.isBefore(assignment.getClosesAt())) status = "CLOSED";
            else status = "OPEN";
            boolean canSubmit = !GRADED.equals(latest == null ? null : latest.getStatus())
                    && !current.isBefore(assignment.getOpensAt()) && current.isBefore(assignment.getClosesAt());
            return new PdfExamStudentRow(assignment, latest, history, status, canSubmit);
        }).sorted(Comparator.comparing(row -> row.assignment().getClosesAt())).toList();
    }

    @Transactional(readOnly = true)
    public SecureExamFile pdfForStudent(Long studentUserId, Long assignmentId) {
        Student student = studentForUser(studentUserId);
        ExamAssignment assignment = requireStudentPdfAssignment(student, assignmentId);
        return storedFile(assignment.getPdfStoredFilename(), "application/pdf", assignment.getPdfOriginalFilename(), ".pdf");
    }

    @Transactional(readOnly = true)
    public SecureExamFile pdfForTeacher(Long teacherUserId, Long assignmentId) {
        ExamAssignment assignment = requireTeacherPdfAssignment(teacherUserId, assignmentId);
        return storedFile(assignment.getPdfStoredFilename(), "application/pdf", assignment.getPdfOriginalFilename(), ".pdf");
    }

    @Transactional
    public ExamPdfSubmission submitForStudent(Long studentUserId, Long assignmentId, List<MultipartFile> uploads) {
        Student student = studentForUser(studentUserId);
        requireStudentPdfAssignment(student, assignmentId);
        ExamAssignment assignment = assignments.findByIdForUpdate(assignmentId).filter(ExamAssignment::isPdfExam)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề PDF."));
        if (enrollments.findByClassEntityIdAndStudentId(assignment.getClassEntity().getId(), student.getId())
                .filter(this::currentEnrollment).isEmpty()) throw new IllegalArgumentException("Đề không thuộc lớp bạn đang học.");
        LocalDateTime current = now();
        if (current.isBefore(assignment.getOpensAt()) || !current.isBefore(assignment.getClosesAt()))
            throw new IllegalArgumentException("Chỉ nộp bài trong thời hạn làm bài.");
        List<ValidatedUpload> validated = validateImages(uploads);
        List<ExamPdfSubmission> previous = submissions.findByAssignmentIdAndStudentIdOrderByVersionNumberDesc(assignmentId, student.getId());
        if (previous.stream().anyMatch(s -> GRADED.equals(s.getStatus())))
            throw new IllegalArgumentException("Bài đã được chấm, không thể nộp lại.");
        int version = previous.isEmpty() ? 1 : previous.get(0).getVersionNumber() + 1;
        List<Path> written = new ArrayList<>();
        try {
            ExamPdfSubmission submission = new ExamPdfSubmission(); submission.setAssignment(assignment); submission.setStudent(student);
            submission.setVersionNumber(version); submission.setStatus(PENDING); submission.setSubmittedAt(current);
            for (ExamPdfSubmission old : previous) if (PENDING.equals(old.getStatus())) old.setStatus(SUPERSEDED);
            if (!previous.isEmpty()) submissions.saveAll(previous);
            int position = 0;
            for (ValidatedUpload upload : validated) {
                String stored = UUID.randomUUID() + upload.extension();
                written.add(store(stored, upload.bytes()));
                ExamPdfSubmissionFile file = new ExamPdfSubmissionFile(); file.setStoredFilename(stored);
                file.setOriginalFilename(upload.originalFilename()); file.setContentType(upload.contentType());
                file.setFileSize((long) upload.bytes().length); file.setPosition(position++);
                submission.addFile(file);
            }
            ExamPdfSubmission saved = submissions.save(submission);
            notifyTeachers(assignment, "exam-pdf-submission-" + saved.getId(),
                    "Có bài làm đề thi PDF mới được nộp. Hãy đăng nhập cổng giáo viên để xem.");
            return saved;
        } catch (RuntimeException e) {
            written.forEach(this::deleteQuietly);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<PdfExamTeacherRow> submissionsForTeacher(Long teacherUserId, Long assignmentId) {
        requireTeacherPdfAssignment(teacherUserId, assignmentId);
        return submissions.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId).stream()
                .map(submission -> new PdfExamTeacherRow(submission.getStudent(), submission)).toList();
    }

    @Transactional(readOnly = true)
    public SecureExamFile submissionFileForTeacher(Long teacherUserId, Long assignmentId, Long fileId) {
        requireTeacherPdfAssignment(teacherUserId, assignmentId);
        ExamPdfSubmissionFile file = files.findById(fileId).filter(f -> f.getSubmission().getAssignment().getId().equals(assignmentId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh bài làm."));
        return storedFile(file.getStoredFilename(), file.getContentType(), file.getOriginalFilename(), allowedExtensionsForImage(file.getContentType()));
    }

    @Transactional(readOnly = true)
    public SecureExamFile submissionFileForStudent(Long studentUserId, Long assignmentId, Long fileId) {
        Student student = studentForUser(studentUserId);
        requireStudentPdfAssignment(student, assignmentId);
        ExamPdfSubmissionFile file = files.findById(fileId)
                .filter(f -> f.getSubmission().getAssignment().getId().equals(assignmentId)
                        && f.getSubmission().getStudent().getId().equals(student.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh bài làm của bạn."));
        return storedFile(file.getStoredFilename(), file.getContentType(), file.getOriginalFilename(), allowedExtensionsForImage(file.getContentType()));
    }

    @Transactional
    public void grade(Long teacherUserId, Long assignmentId, Long submissionId, BigDecimal score, String feedback) {
        requireTeacherPdfAssignment(teacherUserId, assignmentId);
        assignments.findByIdForUpdate(assignmentId).filter(ExamAssignment::isPdfExam)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề PDF."));
        requireTeacherPdfAssignment(teacherUserId, assignmentId);
        ExamPdfSubmission submission = submissions.findById(submissionId)
                .filter(s -> s.getAssignment().getId().equals(assignmentId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài nộp."));
        if (!PENDING.equals(submission.getStatus())) throw new IllegalArgumentException("Chỉ chấm bài đang ở trạng thái Chờ chấm.");
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(MAX_SCORE) > 0 || score.scale() > 2)
            throw new IllegalArgumentException("Điểm phải nằm trong khoảng 0 đến 10, tối đa 2 chữ số thập phân.");
        if (feedback != null && feedback.length() > 1000) throw new IllegalArgumentException("Nhận xét tối đa 1.000 ký tự.");
        submission.setScore(score); submission.setFeedback(feedback == null || feedback.isBlank() ? null : feedback.trim());
        submission.setStatus(GRADED); submission.setReviewedBy(users.findById(teacherUserId).orElseThrow()); submission.setReviewedAt(now());
        Student student = submission.getStudent();
        String eventKey = "exam-pdf-grade-" + submission.getId();
        String genericMessage = "Có kết quả thi mới trong cổng học tập. Hãy đăng nhập để kiểm tra.";
        if (student.getUser() != null) notifications.notifyUser(student.getUser().getId(), eventKey, "GENERAL", "Thông báo học tập", genericMessage, "/student/exams");
        if (student.getParent() != null && student.getParent().getUser() != null)
            notifications.notifyUser(student.getParent().getUser().getId(), eventKey, "GENERAL", "Thông báo học tập", genericMessage, "/parent/exams");
    }

    @Transactional(readOnly = true)
    public List<PdfExamParentRow> gradedForParent(Long parentUserId, Long childId) {
        User parent = users.findById(parentUserId).filter(u -> u.getRole() != null && "ROLE_PARENT".equals(u.getRole().getName())
                        && Boolean.TRUE.equals(u.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản phụ huynh không hoạt động."));
        List<Student> children = students.findByParentUserId(parent.getId()).stream()
                .filter(s -> childId == null || Objects.equals(s.getId(), childId)).toList();
        if (childId != null && children.isEmpty()) throw new IllegalArgumentException("Hồ sơ học sinh không thuộc tài khoản phụ huynh này.");
        return children.stream().flatMap(child -> submissions.findByStudentIdOrderBySubmittedAtDesc(child.getId()).stream()
                        .filter(s -> GRADED.equals(s.getStatus()) && s.getAssignment().isPdfExam())
                        .map(s -> new PdfExamParentRow(child, s.getAssignment(), s)))
                .sorted(Comparator.comparing(row -> row.submission().getReviewedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<ValidatedUpload> validateImages(List<MultipartFile> uploads) {
        if (uploads == null || uploads.stream().filter(f -> f != null && !f.isEmpty()).count() == 0)
            throw new IllegalArgumentException("Chọn ít nhất một ảnh bài làm.");
        List<ValidatedUpload> result = new ArrayList<>(); long total = 0;
        for (MultipartFile file : uploads) {
            if (file == null || file.isEmpty()) continue;
            byte[] bytes = readAndCheckSize(file);
            String name = cleanName(file);
            String extension = extension(name);
            String contentType;
            if ("jpg".equals(extension) || "jpeg".equals(extension)) {
                if (!isJpeg(bytes)) throw new IllegalArgumentException("Ảnh JPG/JPEG không có nội dung ảnh hợp lệ.");
                contentType = "image/jpeg";
            } else if ("png".equals(extension)) {
                if (!isPng(bytes)) throw new IllegalArgumentException("Ảnh PNG không có nội dung ảnh hợp lệ.");
                contentType = "image/png";
            } else throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, JPEG hoặc PNG.");
            total += bytes.length;
            if (total > MAX_FILE_SIZE) throw new IllegalArgumentException("Tổng dung lượng ảnh trong một lần nộp không vượt quá 20 MB.");
            result.add(new ValidatedUpload(name, "." + extension, contentType, bytes));
        }
        return result;
    }

    private ValidatedUpload validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Chọn file PDF đề thi.");
        byte[] bytes = readAndCheckSize(file); String name = cleanName(file);
        if (!"pdf".equals(extension(name)) || bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F' || bytes[4] != '-')
            throw new IllegalArgumentException("Tệp tải lên không phải PDF hợp lệ.");
        return new ValidatedUpload(name, ".pdf", "application/pdf", bytes);
    }

    private byte[] readAndCheckSize(MultipartFile file) {
        if (file.getSize() <= 0 || file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Mỗi tệp không được vượt quá 20 MB.");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > MAX_FILE_SIZE) throw new IllegalArgumentException("Kích thước tệp không hợp lệ.");
            return bytes;
        } catch (IOException e) { throw new IllegalArgumentException("Không đọc được tệp tải lên.", e); }
    }

    private Path store(String filename, byte[] bytes) {
        try {
            Files.createDirectories(storage);
            Path root = storage.toAbsolutePath().normalize(); Path target = root.resolve(filename).normalize();
            if (!target.startsWith(root)) throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ.");
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return target;
        } catch (IOException e) { throw new IllegalArgumentException("Không thể lưu tệp đề thi.", e); }
    }

    private SecureExamFile storedFile(String stored, String contentType, String original, String expectedExtension) {
        String uuidPattern = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
        boolean validName = stored != null && (".jpg|.jpeg".equals(expectedExtension)
                ? stored.matches(uuidPattern + "\\.(jpg|jpeg)")
                : stored.matches(uuidPattern + java.util.regex.Pattern.quote(expectedExtension)));
        if (!validName) throw new IllegalArgumentException("Tệp đề thi không hợp lệ.");
        String extension = stored.substring(stored.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String canonicalType = switch (extension) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            default -> throw new IllegalArgumentException("Loại tệp đề thi không hợp lệ.");
        };
        if (contentType != null && !canonicalType.equals(contentType))
            throw new IllegalArgumentException("Tệp đề thi không hợp lệ.");
        Path root = storage.toAbsolutePath().normalize(); Path path = root.resolve(stored).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Tệp đề thi không còn tồn tại.");
        try {
            long size = Files.size(path);
            if (size <= 0 || size > MAX_FILE_SIZE || !validStoredSignature(path, canonicalType))
                throw new IllegalArgumentException("Tệp đề thi không hợp lệ hoặc đã bị thay đổi.");
        } catch (IOException e) { throw new IllegalArgumentException("Không thể đọc tệp đề thi.", e); }
        return new SecureExamFile(path, canonicalType, original == null ? "document" : original);
    }

    private ExamAssignment requireTeacherPdfAssignment(Long teacherUserId, Long assignmentId) {
        User teacherUser = users.findById(teacherUserId).filter(u -> u.getRole() != null && "ROLE_TEACHER".equals(u.getRole().getName())
                        && Boolean.TRUE.equals(u.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản giáo viên không hoạt động."));
        teachers.findByUserId(teacherUser.getId()).orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có hồ sơ giáo viên."));
        return assignments.findById(assignmentId).filter(ExamAssignment::isPdfExam)
                .filter(a -> ownsClass(teacherUserId, a.getClassEntity()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề PDF hoặc bạn không phụ trách lớp này."));
    }

    private ExamAssignment requireStudentPdfAssignment(Student student, Long assignmentId) {
        ExamAssignment assignment = assignments.findById(assignmentId).filter(ExamAssignment::isPdfExam)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề PDF."));
        if (enrollments.findByClassEntityIdAndStudentId(assignment.getClassEntity().getId(), student.getId())
                .filter(this::currentEnrollment).isEmpty()) throw new IllegalArgumentException("Đề không thuộc lớp bạn đang học.");
        return assignment;
    }

    private ClassEntity requireTeacherClass(Long teacherUserId, Long classId) {
        if (classId == null) throw new IllegalArgumentException("Chọn lớp được giao đề.");
        return classes.findById(classId).filter(c -> ownsClass(teacherUserId, c) && "ACTIVE".equals(c.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Bạn không phụ trách lớp đang hoạt động này."));
    }

    private boolean ownsClass(Long teacherUserId, ClassEntity classEntity) {
        return classEntity != null && ((classEntity.getTeacher() != null && classEntity.getTeacher().getUser() != null
                && Objects.equals(classEntity.getTeacher().getUser().getId(), teacherUserId))
                || (classEntity.getAssistantTeacher() != null && classEntity.getAssistantTeacher().getUser() != null
                && Objects.equals(classEntity.getAssistantTeacher().getUser().getId(), teacherUserId)));
    }

    private Student studentForUser(Long studentUserId) {
        User user = users.findById(studentUserId).filter(u -> u.getRole() != null && "ROLE_STUDENT".equals(u.getRole().getName())
                        && Boolean.TRUE.equals(u.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản học sinh không hoạt động."));
        return students.findByUserId(user.getId()).filter(s -> Boolean.TRUE.equals(s.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa liên kết với hồ sơ học sinh đang hoạt động."));
    }

    private boolean currentEnrollment(ClassStudent e) {
        LocalDate today = LocalDate.now(APP_ZONE);
        return "ACTIVE".equals(e.getStatus()) && (e.getEnrolledDate() == null || !e.getEnrolledDate().isAfter(today))
                && (e.getDroppedDate() == null || e.getDroppedDate().isAfter(today));
    }

    private void notifyClass(ExamAssignment assignment, String eventKey, String message) {
        List<Student> roster = enrollments.findByClassEntityIdAndStatus(assignment.getClassEntity().getId(), "ACTIVE").stream()
                .filter(this::currentEnrollment).map(ClassStudent::getStudent).filter(Objects::nonNull)
                .filter(s -> Boolean.TRUE.equals(s.getActive()) && s.getUser() != null && Boolean.TRUE.equals(s.getUser().getActive())).toList();
        notifications.notifyUsers(roster.stream().map(Student::getUser).map(User::getId).distinct().toList(), eventKey,
                "GENERAL", "Thông báo bài thi", message, "/student/exams");
        notifications.notifyUsers(roster.stream().map(Student::getParent).filter(Objects::nonNull).map(Parent::getUser)
                        .filter(Objects::nonNull).map(User::getId).distinct().toList(), eventKey,
                "GENERAL", "Thông báo bài thi", message, "/parent/exams");
    }

    private void notifyTeachers(ExamAssignment assignment, String eventKey, String message) {
        List<Long> teacherIds = java.util.stream.Stream.of(assignment.getClassEntity().getTeacher(), assignment.getClassEntity().getAssistantTeacher())
                .filter(Objects::nonNull).map(Teacher::getUser).filter(Objects::nonNull).map(User::getId).distinct().toList();
        notifications.notifyUsers(teacherIds, eventKey, "GENERAL", "Bài thi mới được nộp", message, "/teacher/exams/assignments/" + assignment.getId());
    }

    private LocalDateTime now() { return LocalDateTime.now(APP_ZONE).withNano(0); }
    private String titleFromFilename(String original) {
        String value = original == null ? "" : original;
        int dot = value.lastIndexOf('.'); return dot > 0 ? value.substring(0, dot).trim() : value.trim();
    }
    private String cleanName(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isBlank() || name.length() > 255) throw new IllegalArgumentException("Tên tệp không hợp lệ.");
        return name;
    }
    private String extension(String name) {
        int dot = name.lastIndexOf('.'); return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
    private String allowedExtensionsForImage(String contentType) {
        if ("image/png".equals(contentType)) return ".png";
        if ("image/jpeg".equals(contentType)) return ".jpg|.jpeg";
        throw new IllegalArgumentException("Loại ảnh bài làm không hợp lệ.");
    }
    private boolean validStoredSignature(Path path, String contentType) throws IOException {
        try (var input = Files.newInputStream(path)) {
            byte[] b = input.readNBytes(8);
            if ("application/pdf".equals(contentType)) return b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-';
            if ("image/jpeg".equals(contentType)) return b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff;
            if ("image/png".equals(contentType)) return isPng(b);
            return false;
        }
    }
    private boolean isJpeg(byte[] b) { return b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff; }
    private boolean isPng(byte[] b) {
        byte[] sig = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
        if (b.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) if (b[i] != sig[i]) return false;
        return true;
    }
    private void deleteQuietly(Path path) { try { Files.deleteIfExists(path); } catch (IOException ignored) { } }
    private record ValidatedUpload(String originalFilename, String extension, String contentType, byte[] bytes) { }
}
