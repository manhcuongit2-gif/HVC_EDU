package com.educenter.service;

import com.educenter.dto.ParentDTO;
import com.educenter.entity.Parent;

import java.util.List;
import java.util.Optional;

public interface ParentService {
    List<Parent> findAll();
    List<Parent> findAllActive();
    Optional<Parent> findById(Long id);
    Parent createParent(ParentDTO dto);
    Parent updateParent(Long id, ParentDTO dto);
    void toggleActive(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
