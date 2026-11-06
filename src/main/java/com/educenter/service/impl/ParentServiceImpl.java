package com.educenter.service.impl;

import com.educenter.dto.ParentDTO;
import com.educenter.entity.Parent;
import com.educenter.entity.Role;
import com.educenter.entity.User;
import com.educenter.repository.ParentRepository;
import com.educenter.repository.RoleRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.ParentService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ParentServiceImpl implements ParentService {

    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public ParentServiceImpl(ParentRepository parentRepository,
                             UserRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder) {
        this.parentRepository = parentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<Parent> findAll() {
        return parentRepository.findAllByOrderByIdAsc();
    }

    @Override
    public List<Parent> findAllActive() {
        return parentRepository.findByUserActiveTrue();
    }

    @Override
    public Optional<Parent> findById(Long id) {
        return parentRepository.findById(id);
    }

    @Override
    @Transactional
    public Parent createParent(ParentDTO dto) {
        Role parentRole = roleRepository.findByName("ROLE_PARENT")
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình ROLE_PARENT trong hệ thống"));

        // Tạo User
        User user = new User();
        user.setUsername(dto.getUsername());
        String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) 
                ? dto.getPassword() : "123456";
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        user.setRole(parentRole);
        user.setActive(dto.getActive() != null ? dto.getActive() : true);
        User savedUser = userRepository.save(user);

        // Tạo hồ sơ Phụ huynh
        Parent parent = new Parent();
        parent.setUser(savedUser);
        parent.setOccupation(dto.getOccupation());
        parent.setNote(dto.getNote());

        return parentRepository.save(parent);
    }

    @Override
    @Transactional
    public Parent updateParent(Long id, ParentDTO dto) {
        Parent parent = parentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ huynh với ID: " + id));

        User user = parent.getUser();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());

        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        userRepository.save(user);

        parent.setOccupation(dto.getOccupation());
        parent.setNote(dto.getNote());

        return parentRepository.save(parent);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Parent parent = parentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ huynh với ID: " + id));
        User user = parent.getUser();
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
