package com.educenter.service.impl;

import com.educenter.dto.ClassDTO;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassStudent;
import com.educenter.entity.Student;
import com.educenter.entity.Subject;
import com.educenter.entity.Teacher;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.repository.SubjectRepository;
import com.educenter.repository.TeacherRepository;
import com.educenter.service.ClassService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public ClassServiceImpl(ClassRepository classRepository,
                            ClassStudentRepository classStudentRepository,
                            SubjectRepository subjectRepository,
                            TeacherRepository teacherRepository,
                            StudentRepository studentRepository) {
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public List<ClassEntity> getAllClasses() {
        return classRepository.findAllByOrderByIdDesc();
    }

    @Override
    public List<ClassEntity> getActiveClasses() {
        return classRepository.findByStatus("ACTIVE");
    }

    @Override
    public ClassEntity getClassById(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học với ID: " + id));
    }

    @Override
    @Transactional
    public ClassEntity saveClass(ClassDTO classDTO) {
        if (classRepository.existsByName(classDTO.getName())) {
            throw new IllegalArgumentException("Tên lớp học '" + classDTO.getName() + "' đã tồn tại");
        }

        Subject subject = subjectRepository.findById(classDTO.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học"));
        Teacher teacher = teacherRepository.findById(classDTO.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giáo viên"));

        ClassEntity c = new ClassEntity();
        c.setName(classDTO.getName());
        c.setSubject(subject);
        c.setTeacher(teacher);
        c.setMaxStudents(classDTO.getMaxStudents());
        c.setTuitionFee(classDTO.getTuitionFee());
        c.setStatus(classDTO.getStatus() != null ? classDTO.getStatus() : "ACTIVE");
        c.setStartDate(classDTO.getStartDate());
        c.setEndDate(classDTO.getEndDate());
        c.setDescription(classDTO.getDescription());

        return classRepository.save(c);
    }

    @Override
    @Transactional
    public ClassEntity updateClass(Long id, ClassDTO classDTO) {
        ClassEntity c = getClassById(id);

        if (classRepository.existsByNameAndIdNot(classDTO.getName(), id)) {
            throw new IllegalArgumentException("Tên lớp học '" + classDTO.getName() + "' đã được sử dụng");
        }

        Subject subject = subjectRepository.findById(classDTO.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học"));
        Teacher teacher = teacherRepository.findById(classDTO.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giáo viên"));

        c.setName(classDTO.getName());
        c.setSubject(subject);
        c.setTeacher(teacher);
        c.setMaxStudents(classDTO.getMaxStudents());
        c.setTuitionFee(classDTO.getTuitionFee());
        c.setStatus(classDTO.getStatus());
        c.setStartDate(classDTO.getStartDate());
        c.setEndDate(classDTO.getEndDate());
        c.setDescription(classDTO.getDescription());

        return classRepository.save(c);
    }

    @Override
    @Transactional
    public void deleteClass(Long id) {
        if (!classRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy lớp học với ID: " + id);
        }
        classRepository.deleteById(id);
    }

    @Override
    public long getStudentCountInClass(Long classId) {
        return classStudentRepository.countByClassEntityIdAndStatus(classId, "ACTIVE");
    }

    @Override
    public List<ClassStudent> getStudentsInClass(Long classId) {
        return classStudentRepository.findByClassEntityIdAndStatus(classId, "ACTIVE");
    }

    @Override
    @Transactional
    public void addStudentToClass(Long classId, Long studentId) {
        ClassEntity c = getClassById(classId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học sinh với ID: " + studentId));

        long currentCount = classStudentRepository.countByClassEntityIdAndStatus(classId, "ACTIVE");
        if (currentCount >= c.getMaxStudents()) {
            throw new IllegalArgumentException("Lớp học đã đủ sĩ số tối đa (" + c.getMaxStudents() + " học sinh)");
        }

        var existing = classStudentRepository.findByClassEntityIdAndStudentId(classId, studentId);
        if (existing.isPresent()) {
            ClassStudent cs = existing.get();
            if ("ACTIVE".equals(cs.getStatus())) {
                throw new IllegalArgumentException("Học sinh này đã có trong danh sách lớp");
            } else {
                cs.setStatus("ACTIVE");
                cs.setEnrolledDate(LocalDate.now());
                classStudentRepository.save(cs);
                return;
            }
        }

        ClassStudent cs = new ClassStudent(c, student, LocalDate.now(), "ACTIVE");
        classStudentRepository.save(cs);
    }

    @Override
    @Transactional
    public void removeStudentFromClass(Long classId, Long studentId) {
        var existing = classStudentRepository.findByClassEntityIdAndStudentId(classId, studentId);
        if (existing.isPresent()) {
            classStudentRepository.delete(existing.get());
        }
    }

    @Override
    public boolean isClassNameTaken(String name, Long excludeId) {
        if (excludeId == null) {
            return classRepository.existsByName(name);
        }
        return classRepository.existsByNameAndIdNot(name, excludeId);
    }
}
