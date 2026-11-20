package com.educenter.service.impl;

import com.educenter.entity.Attendance;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.Student;
import com.educenter.entity.User;
import com.educenter.repository.AttendanceRepository;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.AttendanceService;
import com.educenter.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AttendanceServiceImpl(AttendanceRepository attendanceRepository,
                                 ClassRepository classRepository,
                                 StudentRepository studentRepository,
                                 UserRepository userRepository,
                                 NotificationService notificationService) {
        this.attendanceRepository = attendanceRepository;
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public List<Attendance> getAttendanceByClassAndDate(Long classId, LocalDate date) {
        return attendanceRepository.findByClassEntityIdAndAttendanceDate(classId, date);
    }

    @Override
    public List<Attendance> getAttendanceByStudent(Long studentId) {
        return attendanceRepository.findByStudentIdOrderByAttendanceDateDesc(studentId);
    }

    @Override
    @Transactional
    public void saveAttendanceBatch(Long classId, LocalDate date, List<Long> studentIds, List<String> statuses, List<String> notes, Long teacherUserId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));
        User user = teacherUserId != null ? userRepository.findById(teacherUserId).orElse(null) : null;

        for (int i = 0; i < studentIds.size(); i++) {
            Long studentId = studentIds.get(i);
            String status = (statuses != null && i < statuses.size()) ? statuses.get(i) : "PRESENT";
            String note = (notes != null && i < notes.size()) ? notes.get(i) : null;

            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) continue;

            Attendance att = attendanceRepository.findByClassEntityIdAndStudentIdAndAttendanceDate(classId, studentId, date)
                    .orElseGet(() -> {
                        Attendance a = new Attendance();
                        a.setClassEntity(classEntity);
                        a.setStudent(student);
                        a.setAttendanceDate(date);
                        return a;
                    });

            att.setStatus(status);
            att.setNote(note);
            att.setCreatedBy(user);
            attendanceRepository.save(att);
            if (student.getParent() != null && student.getParent().getUser() != null) {
                notificationService.notifyUser(student.getParent().getUser().getId(),
                        "attendance-" + classId + "-" + studentId + "-" + date + "-" + status,
                        "ATTENDANCE", "Điểm danh đã được cập nhật", student.getFullName() + " · " + date + " · " + status,
                        "/parent/results");
            }
        }
    }
}
