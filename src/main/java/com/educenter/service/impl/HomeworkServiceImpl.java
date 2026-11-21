package com.educenter.service.impl;

import com.educenter.dto.ParentHomeworkRow;
import com.educenter.dto.StudentHomeworkRow;
import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.HomeworkService;
import com.educenter.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class HomeworkServiceImpl implements HomeworkService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_TEXT_LENGTH = 10_000;
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "jpg", "jpeg", "png", "webp", "zip");
    private final ClassRepository classes;
    private final ClassStudentRepository enrollments;
    private final HomeworkAssignmentRepository assignments;
    private final HomeworkSubmissionRepository submissions;
    private final StudentRepository students;
    private final UserRepository users;
    private final NotificationService notifications;
    private final Path storage;

    public HomeworkServiceImpl(ClassRepository classes, ClassStudentRepository enrollments,
                               HomeworkAssignmentRepository assignments, HomeworkSubmissionRepository submissions,
                               StudentRepository students, UserRepository users, NotificationService notifications,
                               @Value("${app.storage.upload-dir:./uploads}") String uploadDir) {
        this.classes = classes; this.enrollments = enrollments; this.assignments = assignments; this.submissions = submissions;
        this.students = students; this.users = users; this.notifications = notifications;
        this.storage = Path.of(uploadDir).toAbsolutePath().normalize().resolve("homework").normalize();
    }

    @Override @Transactional(readOnly = true)
    public List<HomeworkAssignment> findForTeacher(Long teacherUserId) {
        List<Long> ids = teacherClassIds(teacherUserId);
        return ids.isEmpty() ? List.of() : assignments.findByClassEntityIdInOrderByDueAtDesc(ids);
    }

    @Override @Transactional
    public HomeworkAssignment createForTeacher(Long teacherUserId, Long classId, String title, String instructions, LocalDateTime dueAt, MultipartFile attachment) {
        ClassEntity classEntity = teacherClass(teacherUserId, classId);
        if (title == null || title.isBlank() || title.trim().length() > 150) throw new IllegalArgumentException("Tên bài tập bắt buộc, tối đa 150 ký tự.");
        if (instructions == null || instructions.isBlank() || instructions.length() > 5000) throw new IllegalArgumentException("Vui lòng nhập hướng dẫn bài tập (tối đa 5.000 ký tự).");
        if (dueAt == null) throw new IllegalArgumentException("Vui lòng chọn hạn nộp.");
        HomeworkAssignment a = new HomeworkAssignment(); a.setClassEntity(classEntity); a.setCreatedBy(users.findById(teacherUserId).orElseThrow());
        a.setTitle(title.trim()); a.setInstructions(instructions.trim()); a.setDueAt(dueAt); a.setPublishedAt(LocalDateTime.now());
        if (attachment != null && !attachment.isEmpty()) attachFile(attachment, a);
        a = assignments.save(a);
        List<ClassStudent> roster = enrollments.findByClassEntityIdAndStatus(classId, "ACTIVE").stream()
                .filter(this::isCurrentEnrollment).toList();
        List<Long> parents = roster.stream().map(ClassStudent::getStudent).filter(Objects::nonNull).map(Student::getParent).filter(Objects::nonNull)
                .map(Parent::getUser).filter(Objects::nonNull).map(User::getId).distinct().toList();
        List<Long> studentUsers = roster.stream().map(ClassStudent::getStudent).filter(Objects::nonNull)
                .filter(s -> Boolean.TRUE.equals(s.getActive()) && s.getUser() != null && Boolean.TRUE.equals(s.getUser().getActive()))
                .map(Student::getUser).map(User::getId).distinct().toList();
        notifications.notifyUsers(parents, "homework-assignment-" + a.getId(), "HOMEWORK", "Bài tập mới: " + a.getTitle(),
                "Lớp " + classEntity.getName() + " có bài tập mới, hạn nộp " + dueAt.toString().replace('T', ' '), "/parent/homework");
        notifications.notifyUsers(studentUsers, "homework-assignment-" + a.getId(), "HOMEWORK", "Bài tập mới",
                "Lớp " + classEntity.getName() + " có bài tập mới. Hạn nộp: " + dueAt.toString().replace('T', ' '), "/student/homework");
        return a;
    }

    @Override @Transactional(readOnly = true)
    public List<HomeworkSubmission> submissionsForTeacher(Long teacherUserId, Long assignmentId) {
        requireTeacherAssignment(teacherUserId, assignmentId);
        return submissions.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
    }

    @Override @Transactional
    public HomeworkSubmission reviewForTeacher(Long teacherUserId, Long assignmentId, Long submissionId, BigDecimal score, String feedback) {
        HomeworkAssignment assignment = requireTeacherAssignment(teacherUserId, assignmentId);
        HomeworkSubmission submission = submissions.findByIdAndAssignmentId(submissionId, assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài nộp."));
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0)
            throw new IllegalArgumentException("Điểm bài tập phải từ 0 đến 10.");
        if (feedback != null && feedback.length() > 1000) throw new IllegalArgumentException("Nhận xét tối đa 1.000 ký tự.");
        submission.setScore(score); submission.setFeedback(feedback == null || feedback.isBlank() ? null : feedback.trim());
        submission.setStatus("REVIEWED"); submission.setReviewedBy(users.findById(teacherUserId).orElseThrow()); submission.setReviewedAt(LocalDateTime.now());
        HomeworkSubmission saved = submissions.save(submission);
        User parent = saved.getStudent().getParent().getUser();
        notifications.notifyUser(parent.getId(), "homework-reviewed-" + saved.getId() + "-" + saved.getReviewedAt(), "HOMEWORK",
                "Bài tập đã được chấm", "Giáo viên đã nhận xét bài tập " + assignment.getTitle() + " của " + saved.getStudent().getFullName() + ".", "/parent/homework");
        User studentUser = saved.getStudent().getUser();
        if (studentUser != null && Boolean.TRUE.equals(studentUser.getActive())) {
            notifications.notifyUser(studentUser.getId(), "homework-reviewed-" + saved.getId() + "-" + saved.getReviewedAt(), "HOMEWORK",
                    "Bài tập đã được chấm", "Giáo viên đã chấm và gửi nhận xét cho bài tập của bạn.", "/student/homework");
        }
        return saved;
    }

    @Override @Transactional(readOnly = true)
    public Path assignmentFileForTeacher(Long teacherUserId, Long assignmentId) {
        HomeworkAssignment a = requireTeacherAssignment(teacherUserId, assignmentId); return requiredAssignmentFile(a);
    }

    @Override @Transactional(readOnly = true)
    public Path submissionFileForTeacher(Long teacherUserId, Long assignmentId, Long submissionId) {
        requireTeacherAssignment(teacherUserId, assignmentId);
        HomeworkSubmission s = submissions.findByIdAndAssignmentId(submissionId, assignmentId).orElseThrow(); return requiredSubmissionFile(s);
    }

    @Override @Transactional(readOnly = true)
    public List<ParentHomeworkRow> findForParent(Long parentUserId) {
        List<ClassStudent> childEnrollments = enrollments.findByStudentParentUserId(parentUserId).stream().filter(e -> "ACTIVE".equals(e.getStatus())).toList();
        if (childEnrollments.isEmpty()) return List.of();
        List<Long> classIds = childEnrollments.stream().map(e -> e.getClassEntity().getId()).distinct().toList();
        List<HomeworkAssignment> tasks = assignments.findByClassEntityIdInAndPublishedAtLessThanEqualOrderByDueAtAsc(classIds, LocalDateTime.now());
        List<Long> taskIds = tasks.stream().map(HomeworkAssignment::getId).toList();
        Map<String, HomeworkSubmission> byPair = new HashMap<>();
        if (!taskIds.isEmpty()) submissions.findByStudentParentUserIdAndAssignmentIdIn(parentUserId, taskIds).forEach(s -> byPair.put(key(s.getAssignment().getId(), s.getStudent().getId()), s));
        List<ParentHomeworkRow> rows = new ArrayList<>();
        for (ClassStudent enrollment : childEnrollments) for (HomeworkAssignment task : tasks) {
            if (task.getClassEntity().getId().equals(enrollment.getClassEntity().getId()))
                rows.add(new ParentHomeworkRow(task, enrollment.getStudent(), byPair.get(key(task.getId(), enrollment.getStudent().getId()))));
        }
        return rows;
    }

    @Override @Transactional
    public HomeworkSubmission submitForParent(Long parentUserId, Long assignmentId, Long studentId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Chọn tệp bài làm để nộp.");
        Student student = students.findById(studentId).filter(s -> s.getParent() != null && s.getParent().getUser().getId().equals(parentUserId))
                .orElseThrow(() -> new IllegalArgumentException("Bạn không có quyền nộp bài cho học sinh này."));
        HomeworkAssignment assignment = assignments.findById(assignmentId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài tập."));
        boolean enrolled = enrollments.findByClassEntityIdAndStudentId(assignment.getClassEntity().getId(), studentId)
                .filter(e -> "ACTIVE".equals(e.getStatus())).isPresent();
        if (!enrolled) throw new IllegalArgumentException("Học sinh không còn học lớp có bài tập này.");
        HomeworkSubmission submission = submissions.findByAssignmentIdAndStudentId(assignmentId, studentId).orElseGet(HomeworkSubmission::new);
        submission.setAssignment(assignment); submission.setStudent(student); submission.setSubmittedBy(users.findById(parentUserId).orElseThrow());
        String stored = storeFile(file); submission.setAttachmentStoredFilename(stored); submission.setAttachmentOriginalFilename(cleanName(file));
        submission.setSubmissionText(null);
        submission.setAttachmentContentType(file.getContentType()); submission.setAttachmentFileSize(file.getSize()); submission.setSubmittedAt(LocalDateTime.now(APP_ZONE));
        submission.setLate(LocalDateTime.now(APP_ZONE).isAfter(assignment.getDueAt())); submission.setStatus("SUBMITTED");
        submission.setScore(null); submission.setFeedback(null); submission.setReviewedBy(null); submission.setReviewedAt(null);
        HomeworkSubmission saved = submissions.save(submission);
        List<Long> teacherIds = java.util.stream.Stream.of(assignment.getClassEntity().getTeacher(), assignment.getClassEntity().getAssistantTeacher())
                .filter(Objects::nonNull).map(Teacher::getUser).filter(Objects::nonNull).map(User::getId).distinct().toList();
        notifications.notifyUsers(teacherIds, "homework-submission-" + saved.getId() + "-" + saved.getSubmittedAt(), "HOMEWORK",
                "Có bài tập mới được nộp", student.getFullName() + " đã nộp bài " + assignment.getTitle() + (saved.isLate() ? " (nộp muộn)." : "."),
                "/teacher/homework/" + assignmentId + "/submissions");
        return saved;
    }

    @Override @Transactional(readOnly = true)
    public Path assignmentFileForParent(Long parentUserId, Long assignmentId) {
        ensureParentHasAssignment(parentUserId, assignmentId); return requiredAssignmentFile(assignments.findById(assignmentId).orElseThrow());
    }

    @Override @Transactional(readOnly = true)
    public Path submissionFileForParent(Long parentUserId, Long submissionId) {
        HomeworkSubmission s = submissions.findById(submissionId).orElseThrow();
        if (!s.getStudent().getParent().getUser().getId().equals(parentUserId)) throw new IllegalArgumentException("Không có quyền xem bài nộp này.");
        return requiredSubmissionFile(s);
    }

    @Override @Transactional(readOnly = true)
    public List<StudentHomeworkRow> findForStudent(Long studentUserId) {
        Student student = studentForUser(studentUserId);
        List<Long> classIds = enrollments.findByStudentId(student.getId()).stream()
                .filter(this::isCurrentEnrollment)
                .map(e -> e.getClassEntity().getId()).distinct().toList();
        if (classIds.isEmpty()) return List.of();
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        List<HomeworkAssignment> tasks = assignments.findByClassEntityIdInAndPublishedAtLessThanEqualOrderByDueAtAsc(classIds, now);
        if (tasks.isEmpty()) return List.of();
        List<Long> taskIds = tasks.stream().map(HomeworkAssignment::getId).toList();
        Map<Long, HomeworkSubmission> byAssignment = new HashMap<>();
        submissions.findByStudentIdAndAssignmentIdIn(student.getId(), taskIds)
                .forEach(s -> byAssignment.put(s.getAssignment().getId(), s));
        return tasks.stream().map(task -> {
            HomeworkSubmission submission = byAssignment.get(task.getId());
            String status;
            if (submission != null) status = submission.getStatus();
            else status = now.isAfter(task.getDueAt()) ? "OVERDUE" : "PENDING";
            return new StudentHomeworkRow(task, submission, status);
        }).toList();
    }

    @Override @Transactional
    public HomeworkSubmission submitForStudent(Long studentUserId, Long assignmentId, String responseText, MultipartFile file) {
        Student student = studentForUser(studentUserId);
        HomeworkAssignment assignment = requireStudentAssignment(student, assignmentId);
        String text = responseText == null ? "" : responseText.trim();
        boolean hasFile = file != null && !file.isEmpty();
        if (text.length() > MAX_TEXT_LENGTH) throw new IllegalArgumentException("Câu trả lời tối đa 10.000 ký tự.");
        if (text.isBlank() && !hasFile) throw new IllegalArgumentException("Nhập câu trả lời hoặc chọn tệp bài làm để nộp.");

        HomeworkSubmission submission = submissions.findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElseGet(HomeworkSubmission::new);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmittedBy(student.getUser());
        submission.setSubmissionText(text.isBlank() ? null : text);
        if (hasFile) {
            String stored = storeFile(file);
            submission.setAttachmentStoredFilename(stored);
            submission.setAttachmentOriginalFilename(cleanName(file));
            submission.setAttachmentContentType(file.getContentType());
            submission.setAttachmentFileSize(file.getSize());
        } else {
            submission.setAttachmentStoredFilename(null);
            submission.setAttachmentOriginalFilename(null);
            submission.setAttachmentContentType(null);
            submission.setAttachmentFileSize(null);
        }
        LocalDateTime submittedAt = LocalDateTime.now(APP_ZONE);
        submission.setSubmittedAt(submittedAt);
        submission.setLate(submittedAt.isAfter(assignment.getDueAt()));
        submission.setStatus("SUBMITTED");
        submission.setScore(null);
        submission.setFeedback(null);
        submission.setReviewedBy(null);
        submission.setReviewedAt(null);
        HomeworkSubmission saved = submissions.save(submission);
        List<Long> teacherIds = java.util.stream.Stream.of(assignment.getClassEntity().getTeacher(), assignment.getClassEntity().getAssistantTeacher())
                .filter(Objects::nonNull).map(Teacher::getUser).filter(Objects::nonNull).map(User::getId).distinct().toList();
        notifications.notifyUsers(teacherIds, "homework-submission-" + saved.getId() + "-" + submittedAt, "HOMEWORK",
                "Có bài tập mới được nộp", student.getFullName() + " đã nộp bài " + assignment.getTitle() + (saved.isLate() ? " (nộp muộn)." : "."),
                "/teacher/homework/" + assignmentId + "/submissions");
        return saved;
    }

    @Override @Transactional(readOnly = true)
    public Path assignmentFileForStudent(Long studentUserId, Long assignmentId) {
        HomeworkAssignment assignment = requireStudentAssignment(studentForUser(studentUserId), assignmentId);
        return requiredAssignmentFile(assignment);
    }

    @Override @Transactional(readOnly = true)
    public Path submissionFileForStudent(Long studentUserId, Long submissionId) {
        Student student = studentForUser(studentUserId);
        HomeworkSubmission submission = submissions.findById(submissionId)
                .filter(s -> s.getStudent().getId().equals(student.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Không có quyền xem bài nộp này."));
        return requiredSubmissionFile(submission);
    }

    private List<Long> teacherClassIds(Long teacherUserId) {
        return classes.findAll().stream().filter(c -> (c.getTeacher() != null && c.getTeacher().getUser().getId().equals(teacherUserId))
                || (c.getAssistantTeacher() != null && c.getAssistantTeacher().getUser().getId().equals(teacherUserId))).map(ClassEntity::getId).toList();
    }
    private ClassEntity teacherClass(Long teacherUserId, Long classId) {
        return classes.findById(classId).filter(c -> (c.getTeacher() != null && c.getTeacher().getUser().getId().equals(teacherUserId))
                || (c.getAssistantTeacher() != null && c.getAssistantTeacher().getUser().getId().equals(teacherUserId)))
                .orElseThrow(() -> new IllegalArgumentException("Bạn không phụ trách lớp này."));
    }
    private HomeworkAssignment requireTeacherAssignment(Long teacherUserId, Long assignmentId) {
        List<Long> ids = teacherClassIds(teacherUserId);
        if (ids.isEmpty()) throw new IllegalArgumentException("Giáo viên chưa được phân công lớp.");
        return assignments.findByIdAndClassEntityIdIn(assignmentId, ids).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài tập hoặc bạn không phụ trách lớp này."));
    }
    private void ensureParentHasAssignment(Long parentId, Long assignmentId) {
        List<Long> ids = enrollments.findByStudentParentUserId(parentId).stream().filter(e -> "ACTIVE".equals(e.getStatus())).map(e -> e.getClassEntity().getId()).distinct().toList();
        if (ids.isEmpty() || assignments.findByIdAndClassEntityIdIn(assignmentId, ids).isEmpty()) throw new IllegalArgumentException("Bài tập này không thuộc lớp của con bạn.");
    }
    private Student studentForUser(Long userId) {
        Student student = students.findByUserId(userId)
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa được liên kết với hồ sơ học sinh đang hoạt động."));
        if (student.getUser() == null || student.getUser().getRole() == null
                || !"ROLE_STUDENT".equals(student.getUser().getRole().getName())
                || !Boolean.TRUE.equals(student.getUser().getActive())) {
            throw new IllegalArgumentException("Tài khoản học sinh không hoạt động.");
        }
        return student;
    }
    private HomeworkAssignment requireStudentAssignment(Student student, Long assignmentId) {
        HomeworkAssignment assignment = assignments.findById(assignmentId)
                .filter(a -> !a.getPublishedAt().isAfter(LocalDateTime.now(APP_ZONE)))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài tập."));
        boolean enrolled = enrollments.findByClassEntityIdAndStudentId(assignment.getClassEntity().getId(), student.getId())
                .filter(this::isCurrentEnrollment).isPresent();
        if (!enrolled) throw new IllegalArgumentException("Bài tập này không thuộc lớp bạn đang học.");
        return assignment;
    }
    private void attachFile(MultipartFile file, HomeworkAssignment a) {
        String stored = storeFile(file); a.setAttachmentStoredFilename(stored); a.setAttachmentOriginalFilename(cleanName(file));
        a.setAttachmentContentType(file.getContentType()); a.setAttachmentFileSize(file.getSize());
    }
    private String storeFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Tệp không được vượt quá 20 MB.");
        String name = cleanName(file); String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED.contains(extension)) throw new IllegalArgumentException("Định dạng tệp chưa được hỗ trợ.");
        String stored = UUID.randomUUID() + "." + extension;
        try { Files.createDirectories(storage); try (var input = file.getInputStream()) { Files.copy(input, storage.resolve(stored), StandardCopyOption.REPLACE_EXISTING); } }
        catch (IOException e) { throw new IllegalArgumentException("Không thể lưu tệp.", e); }
        return stored;
    }
    private String cleanName(MultipartFile file) {
        String value = file.getOriginalFilename() == null ? "bai-lam" : file.getOriginalFilename().replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).trim();
        if (value.isBlank() || value.length() > 255) throw new IllegalArgumentException("Tên tệp không hợp lệ.");
        return value;
    }
    private Path requiredAssignmentFile(HomeworkAssignment a) { return resolve(a.getAttachmentStoredFilename()); }
    private Path requiredSubmissionFile(HomeworkSubmission s) { return resolve(s.getAttachmentStoredFilename()); }
    private Path resolve(String stored) {
        if (stored == null) throw new IllegalArgumentException("Không có tệp đính kèm.");
        Path path = storage.resolve(stored).normalize();
        if (!path.startsWith(storage) || !Files.isRegularFile(path)) throw new IllegalArgumentException("Tệp không còn tồn tại.");
        return path;
    }
    private String key(Long assignmentId, Long studentId) { return assignmentId + ":" + studentId; }
    private boolean isCurrentEnrollment(ClassStudent enrollment) {
        LocalDate today = LocalDate.now(APP_ZONE);
        return "ACTIVE".equals(enrollment.getStatus())
                && (enrollment.getEnrolledDate() == null || !enrollment.getEnrolledDate().isAfter(today))
                && (enrollment.getDroppedDate() == null || enrollment.getDroppedDate().isAfter(today));
    }
}
