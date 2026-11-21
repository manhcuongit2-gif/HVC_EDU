package com.educenter.service.impl;

import com.educenter.dto.StudentDTO;
import com.educenter.entity.Parent;
import com.educenter.entity.Role;
import com.educenter.entity.Student;
import com.educenter.entity.User;
import com.educenter.repository.ParentRepository;
import com.educenter.repository.RoleRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.StudentService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentServiceImpl(StudentRepository studentRepository,
                              ParentRepository parentRepository,
                              UserRepository userRepository,
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<Student> findAll() {
        return studentRepository.findAllByOrderByIdAsc();
    }

    @Override
    public List<Student> findAllActive() {
        return studentRepository.findByActiveTrue();
    }

    @Override
    public List<Student> findByParentId(Long parentId) {
        return studentRepository.findByParentId(parentId);
    }

    @Override
    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    @Override
    @Transactional
    public Student createStudent(StudentDTO dto) {
        Parent parent = parentRepository.findById(dto.getParentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ huynh với ID: " + dto.getParentId()));

        Student student = new Student();
        student.setFullName(dto.getFullName());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setGender(dto.getGender());
        student.setPhone(dto.getPhone());
        student.setSchool(dto.getSchool());
        student.setGradeLevel(dto.getGradeLevel());
        student.setParent(parent);
        student.setActive(dto.getActive() != null ? dto.getActive() : true);

        return studentRepository.save(student);
    }

    @Override
    @Transactional
    public Student updateStudent(Long id, StudentDTO dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh với ID: " + id));

        Parent parent = parentRepository.findById(dto.getParentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ huynh với ID: " + dto.getParentId()));

        student.setFullName(dto.getFullName());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setGender(dto.getGender());
        student.setPhone(dto.getPhone());
        student.setSchool(dto.getSchool());
        student.setGradeLevel(dto.getGradeLevel());
        student.setParent(parent);

        if (student.getUser() != null) {
            student.getUser().setFullName(student.getFullName());
            student.getUser().setPhone(student.getPhone());
            userRepository.save(student.getUser());
        }

        return studentRepository.save(student);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh với ID: " + id));
        student.setActive(!student.getActive());
        if (student.getUser() != null) {
            student.getUser().setActive(student.getActive());
            userRepository.save(student.getUser());
        }
        studentRepository.save(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findUnlinkedStudentAccounts() {
        return userRepository.findByRole_NameAndActiveTrueOrderByFullNameAsc("ROLE_STUDENT").stream()
                .filter(user -> !studentRepository.existsByUserId(user.getId()))
                .toList();
    }

    @Override
    @Transactional
    public void createStudentAccount(Long studentId, String username, String email, String password) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ học sinh."));
        ensureAccountCanBeLinked(student);

        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        if (!normalizedUsername.matches("[A-Za-z0-9._-]{4,50}")) {
            throw new IllegalArgumentException("Tên đăng nhập phải dài 4–50 ký tự, chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        }
        if (normalizedEmail.isBlank() || normalizedEmail.length() > 100
                || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Vui lòng nhập email hợp lệ, tối đa 100 ký tự.");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email đã được đăng ký trong hệ thống.");
        }
        if (password == null || password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
        }

        Role role = roleRepository.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new IllegalStateException("Chưa cấu hình ROLE_STUDENT trong hệ thống."));
        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(student.getFullName());
        user.setPhone(student.getPhone());
        user.setRole(role);
        user.setActive(Boolean.TRUE.equals(student.getActive()));
        student.setUser(userRepository.save(user));
        studentRepository.save(student);
    }

    @Override
    @Transactional
    public void linkStudentAccount(Long studentId, Long userId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ học sinh."));
        ensureAccountCanBeLinked(student);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (user.getRole() == null || !"ROLE_STUDENT".equals(user.getRole().getName())) {
            throw new IllegalArgumentException("Chỉ có thể liên kết tài khoản học sinh.");
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("Không thể liên kết tài khoản đã khóa.");
        }
        if (studentRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("Tài khoản đã được liên kết với hồ sơ học sinh khác.");
        }
        user.setFullName(student.getFullName());
        user.setPhone(student.getPhone());
        user.setActive(Boolean.TRUE.equals(student.getActive()));
        userRepository.save(user);
        student.setUser(user);
        studentRepository.save(student);
    }

    private void ensureAccountCanBeLinked(Student student) {
        if (student.getUser() != null) {
            throw new IllegalArgumentException("Hồ sơ này đã có tài khoản đăng nhập.");
        }
    }
}
