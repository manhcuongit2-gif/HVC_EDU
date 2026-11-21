package com.educenter.service.impl;

import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.NotificationService;
import com.educenter.service.RegistrationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final TuitionRepository tuitionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public RegistrationServiceImpl(RegistrationRepository registrationRepository,
                                   ParentRepository parentRepository,
                                   StudentRepository studentRepository,
                                   ClassRepository classRepository,
                                   ClassStudentRepository classStudentRepository,
                                   TuitionRepository tuitionRepository,
                                   UserRepository userRepository,
                                   NotificationService notificationService) {
        this.registrationRepository = registrationRepository;
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.tuitionRepository = tuitionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public List<Registration> getAllRegistrations() {
        return registrationRepository.findAllByOrderByRegistrationDateDesc();
    }

    @Override
    public List<Registration> getPendingRegistrations() {
        return registrationRepository.findByStatusOrderByRegistrationDateDesc("PENDING");
    }

    @Override
    public List<Registration> getRegistrationsByParentUserId(Long userId) {
        return registrationRepository.findByParentUserIdOrderByRegistrationDateDesc(userId);
    }

    @Override
    @Transactional
    public Registration registerClass(Long parentUserId, Long studentId, Long classId, String note) {
        Parent parent = parentRepository.findByUserId(parentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin phụ huynh"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin học sinh"));
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin lớp học"));

        if (!student.getParent().getId().equals(parent.getId())) {
            throw new IllegalArgumentException("Học sinh này không thuộc quyền quản lý của tài khoản phụ huynh");
        }

        if (classStudentRepository.existsByClassEntityIdAndStudentId(classId, studentId)) {
            throw new IllegalArgumentException("Học sinh này đã tham gia lớp học rồi");
        }

        Registration r = new Registration();
        r.setParent(parent);
        r.setStudent(student);
        r.setClassEntity(classEntity);
        r.setRegistrationDate(LocalDateTime.now());
        r.setStatus("PENDING");
        r.setNote(note);

        Registration saved = registrationRepository.save(r);
        List<Long> adminIds = userRepository.findByRole_NameAndActiveTrueOrderByFullNameAsc("ROLE_ADMIN")
                .stream().map(User::getId).toList();
        notificationService.notifyUsers(adminIds, "registration-submitted-" + saved.getId(), "GENERAL",
                "Có đơn đăng ký học mới", "Có đơn đăng ký lớp cần được xem xét.", "/admin/registrations");
        return saved;
    }

    @Override
    @Transactional
    public void approveRegistration(Long registrationId, Long adminUserId, String adminNote) {
        Registration r = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đăng ký"));

        if (!"PENDING".equals(r.getStatus())) {
            throw new IllegalArgumentException("Đơn đăng ký này đã được xử lý trước đó");
        }

        ClassEntity classEntity = r.getClassEntity();
        long currentEnrolled = classStudentRepository.countByClassEntityIdAndStatus(classEntity.getId(), "ACTIVE");
        if (currentEnrolled >= classEntity.getMaxStudents()) {
            throw new IllegalArgumentException("Lớp học đã đủ sĩ số tối đa (" + classEntity.getMaxStudents() + " học sinh)");
        }

        User admin = userRepository.findById(adminUserId).orElse(null);
        r.setStatus("APPROVED");
        r.setAdminNote(adminNote != null ? adminNote : "Đã duyệt và thêm vào lớp");
        r.setProcessedBy(admin);
        r.setProcessedDate(LocalDateTime.now());
        registrationRepository.save(r);

        // 1. Thêm vào danh sách lớp
        var existingCs = classStudentRepository.findByClassEntityIdAndStudentId(classEntity.getId(), r.getStudent().getId());
        if (existingCs.isPresent()) {
            ClassStudent cs = existingCs.get();
            cs.setStatus("ACTIVE");
            cs.setEnrolledDate(LocalDate.now());
            classStudentRepository.save(cs);
        } else {
            ClassStudent cs = new ClassStudent(classEntity, r.getStudent(), LocalDate.now(), "ACTIVE");
            classStudentRepository.save(cs);
        }

        // Tuition is created only by the Admin manual billing flow or the existing
        // monthly per-session billing process. Approving enrollment must not create
        // a second course-based charge.
        notificationService.notifyUser(r.getParent().getUser().getId(), "registration-processed-" + r.getId(), "GENERAL",
                "Đăng ký lớp đã được duyệt", "Đơn đăng ký lớp " + classEntity.getName() + " đã được duyệt.",
                "/parent/registrations");
    }

    @Override
    @Transactional
    public void rejectRegistration(Long registrationId, Long adminUserId, String adminNote) {
        Registration r = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đăng ký"));

        if (!"PENDING".equals(r.getStatus())) {
            throw new IllegalArgumentException("Đơn đăng ký này đã được xử lý trước đó");
        }

        User admin = userRepository.findById(adminUserId).orElse(null);
        r.setStatus("REJECTED");
        r.setAdminNote(adminNote != null ? adminNote : "Trung tâm tạm thời chưa thể tiếp nhận đơn đăng ký này");
        r.setProcessedBy(admin);
        r.setProcessedDate(LocalDateTime.now());
        registrationRepository.save(r);
        notificationService.notifyUser(r.getParent().getUser().getId(), "registration-processed-" + r.getId(), "GENERAL",
                "Đăng ký lớp chưa được duyệt", "Đơn đăng ký lớp " + r.getClassEntity().getName() + " chưa được tiếp nhận.",
                "/parent/registrations");
    }

    @Override
    public long getPendingCount() {
        return registrationRepository.countByStatus("PENDING");
    }
}
