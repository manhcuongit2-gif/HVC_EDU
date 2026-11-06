package com.educenter.service.impl;

import com.educenter.dto.TeacherDTO;
import com.educenter.entity.Role;
import com.educenter.entity.Teacher;
import com.educenter.entity.User;
import com.educenter.repository.RoleRepository;
import com.educenter.repository.TeacherRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.TeacherService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherServiceImpl(TeacherRepository teacherRepository,
                              UserRepository userRepository,
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<Teacher> findAll() {
        return teacherRepository.findAllByOrderByIdAsc();
    }

    @Override
    public List<Teacher> findAllActive() {
        return teacherRepository.findByUserActiveTrue();
    }

    @Override
    public Optional<Teacher> findById(Long id) {
        return teacherRepository.findById(id);
    }

    @Override
    @Transactional
    public Teacher createTeacher(TeacherDTO dto) {
        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình ROLE_TEACHER trong hệ thống"));

        // Tạo tài khoản User
        User user = new User();
        user.setUsername(dto.getUsername());
        String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) 
                ? dto.getPassword() : "123456";
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        user.setRole(teacherRole);
        user.setActive(dto.getActive() != null ? dto.getActive() : true);
        User savedUser = userRepository.save(user);

        // Tạo hồ sơ Giáo viên
        Teacher teacher = new Teacher();
        teacher.setUser(savedUser);
        teacher.setSpecialization(dto.getSpecialization());
        teacher.setDegree(dto.getDegree());
        teacher.setExperience(dto.getExperience());

        return teacherRepository.save(teacher);
    }

    @Override
    @Transactional
    public Teacher updateTeacher(Long id, TeacherDTO dto) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên với ID: " + id));

        User user = teacher.getUser();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());

        // Đổi mật khẩu nếu người dùng nhập mật khẩu mới
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        userRepository.save(user);

        teacher.setSpecialization(dto.getSpecialization());
        teacher.setDegree(dto.getDegree());
        teacher.setExperience(dto.getExperience());

        return teacherRepository.save(teacher);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên với ID: " + id));
        User user = teacher.getUser();
        user.setActive(!user.getActive());
        userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
