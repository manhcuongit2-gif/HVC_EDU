package com.educenter.service.impl;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.Grade;
import com.educenter.entity.Student;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.GradeRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.service.GradeService;
import com.educenter.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notifications;

    public GradeServiceImpl(GradeRepository gradeRepository,
                            ClassRepository classRepository,
                            StudentRepository studentRepository,
                            NotificationService notifications) {
        this.gradeRepository = gradeRepository;
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
        this.notifications = notifications;
    }

    @Override
    public List<Grade> getGradesByClassId(Long classId) {
        return gradeRepository.findByClassEntityId(classId);
    }

    @Override
    public List<Grade> getGradesByStudentId(Long studentId) {
        return gradeRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional
    public void saveGrade(Long classId, Long studentId, BigDecimal testScore, BigDecimal midtermScore, BigDecimal finalScore, String note) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học sinh"));

        Grade grade = gradeRepository.findByClassEntityIdAndStudentId(classId, studentId)
                .orElseGet(() -> {
                    Grade g = new Grade();
                    g.setClassEntity(classEntity);
                    g.setStudent(student);
                    return g;
                });

        grade.setTestScore(testScore);
        grade.setMidtermScore(midtermScore);
        grade.setFinalScore(finalScore);
        grade.setNote(note);
        grade.recalculateAverageScore();

        Grade saved = gradeRepository.save(grade);
        String eventKey = "grade-updated-" + classId + "-" + studentId + "-" + java.util.UUID.randomUUID();
        if (student.getUser() != null && Boolean.TRUE.equals(student.getUser().getActive())) {
            notifications.notifyUser(student.getUser().getId(), eventKey, "LEARNING_REPORT", "Điểm học tập đã được cập nhật",
                    "Giáo viên vừa cập nhật điểm hoặc nhận xét trong hồ sơ học tập của bạn.", "/student/progress");
        }
        if (student.getParent() != null && student.getParent().getUser() != null) {
            notifications.notifyUser(student.getParent().getUser().getId(), eventKey, "LEARNING_REPORT", "Điểm học tập đã được cập nhật",
                    "Giáo viên vừa cập nhật kết quả học tập của " + student.getFullName() + ".", "/parent/results");
        }
    }

    @Override
    @Transactional
    public void saveGradeBatch(Long classId, List<Long> studentIds, List<BigDecimal> testScores, List<BigDecimal> midtermScores, List<BigDecimal> finalScores, List<String> notes) {
        for (int i = 0; i < studentIds.size(); i++) {
            Long studentId = studentIds.get(i);
            BigDecimal test = (testScores != null && i < testScores.size()) ? testScores.get(i) : null;
            BigDecimal mid = (midtermScores != null && i < midtermScores.size()) ? midtermScores.get(i) : null;
            BigDecimal fin = (finalScores != null && i < finalScores.size()) ? finalScores.get(i) : null;
            String note = (notes != null && i < notes.size()) ? notes.get(i) : null;

            saveGrade(classId, studentId, test, mid, fin, note);
        }
    }
}
