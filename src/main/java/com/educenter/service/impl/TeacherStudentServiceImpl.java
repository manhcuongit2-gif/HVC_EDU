package com.educenter.service.impl;

import com.educenter.dto.TeacherStudentForm;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassStudent;
import com.educenter.entity.Parent;
import com.educenter.entity.Student;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.repository.ParentRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.service.ClassService;
import com.educenter.service.TeacherStudentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeacherStudentServiceImpl implements TeacherStudentService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final ClassService classService;

    public TeacherStudentServiceImpl(ClassRepository classRepository,
                                     ClassStudentRepository classStudentRepository,
                                     StudentRepository studentRepository,
                                     ParentRepository parentRepository,
                                     ClassService classService) {
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.classService = classService;
    }

    @Override
    @Transactional(readOnly = true)
    public ClassEntity getClassForTeacher(Long teacherUserId, Long classId) {
        return requireOwnedClass(teacherUserId, classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassStudent> getRoster(Long teacherUserId, Long classId) {
        requireOwnedClass(teacherUserId, classId);
        return classStudentRepository.findByClassEntityIdAndStatus(classId, "ACTIVE");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getAvailableStudents(Long teacherUserId, Long classId) {
        requireOwnedClass(teacherUserId, classId);
        Set<Long> enrolledIds = classStudentRepository.findByClassEntityIdAndStatus(classId, "ACTIVE")
                .stream().map(row -> row.getStudent().getId()).collect(Collectors.toSet());
        return studentRepository.findByActiveTrue().stream()
                .filter(student -> !enrolledIds.contains(student.getId()))
                .toList();
    }

    @Override
    @Transactional
    public void addExisting(Long teacherUserId, Long classId, Long studentId) {
        requireOwnedClass(teacherUserId, classId);
        Student student = studentRepository.findById(studentId)
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học sinh đang hoạt động."));
        classService.addStudentToClass(classId, student.getId());
    }

    @Override
    @Transactional
    public void createAndEnroll(Long teacherUserId, Long classId, TeacherStudentForm form) {
        ClassEntity classEntity = requireOwnedClass(teacherUserId, classId);
        if (classStudentRepository.countByClassEntityIdAndStatus(classId, "ACTIVE") >= classEntity.getMaxStudents()) {
            throw new IllegalArgumentException("Lớp học đã đủ sĩ số tối đa (" + classEntity.getMaxStudents() + " học sinh)");
        }
        if (form.getParentId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn phụ huynh cho học sinh.");
        }
        Parent parent = parentRepository.findById(form.getParentId())
                .filter(value -> value.getUser() != null && Boolean.TRUE.equals(value.getUser().getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phụ huynh đang hoạt động."));

        Student student = new Student();
        applyForm(student, form);
        student.setParent(parent);
        student.setActive(true);
        Student saved = studentRepository.save(student);
        classService.addStudentToClass(classId, saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Student getStudentForEdit(Long teacherUserId, Long classId, Long studentId) {
        requireActiveEnrollment(teacherUserId, classId, studentId);
        boolean enrolledWithOtherTeacher = classStudentRepository.findByStudentId(studentId).stream()
                .filter(row -> "ACTIVE".equals(row.getStatus()))
                .map(ClassStudent::getClassEntity)
                .filter(Objects::nonNull)
                .map(ClassEntity::getTeacher)
                .filter(Objects::nonNull)
                .map(teacher -> teacher.getUser() == null ? null : teacher.getUser().getId())
                .anyMatch(otherTeacherId -> otherTeacherId != null && !Objects.equals(otherTeacherId, teacherUserId));
        if (enrolledWithOtherTeacher) {
            throw new IllegalArgumentException("Học sinh đang học với người dạy khác; chỉ admin mới được sửa hồ sơ dùng chung.");
        }
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học sinh."));
    }

    @Override
    @Transactional
    public void updateStudent(Long teacherUserId, Long classId, Long studentId, TeacherStudentForm form) {
        Student student = getStudentForEdit(teacherUserId, classId, studentId);
        applyForm(student, form);
        studentRepository.save(student);
    }

    @Override
    @Transactional
    public void dropFromClass(Long teacherUserId, Long classId, Long studentId) {
        requireActiveEnrollment(teacherUserId, classId, studentId);
        classService.removeStudentFromClass(classId, studentId);
    }

    private ClassEntity requireOwnedClass(Long teacherUserId, Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học."));
        if (classEntity.getTeacher() == null || classEntity.getTeacher().getUser() == null
                || !Objects.equals(classEntity.getTeacher().getUser().getId(), teacherUserId)) {
            throw new IllegalArgumentException("Bạn không được phân công phụ trách lớp học này.");
        }
        if (!classEntity.getTeacher().isCanManageStudents()) {
            throw new IllegalArgumentException("Tài khoản chưa được admin cấp quyền quản lý học sinh.");
        }
        return classEntity;
    }

    private void requireActiveEnrollment(Long teacherUserId, Long classId, Long studentId) {
        requireOwnedClass(teacherUserId, classId);
        boolean enrolled = classStudentRepository.findByClassEntityIdAndStudentId(classId, studentId)
                .filter(row -> "ACTIVE".equals(row.getStatus()))
                .isPresent();
        if (!enrolled) throw new IllegalArgumentException("Học sinh không nằm trong lớp đang phụ trách.");
    }

    private void applyForm(Student student, TeacherStudentForm form) {
        student.setFullName(form.getFullName().trim());
        student.setDateOfBirth(form.getDateOfBirth());
        student.setGender(form.getGender());
        student.setPhone(form.getPhone());
        student.setSchool(form.getSchool());
        student.setGradeLevel(form.getGradeLevel());
    }
}
