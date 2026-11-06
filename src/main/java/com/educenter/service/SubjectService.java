package com.educenter.service;

import com.educenter.entity.Subject;

import java.util.List;
import java.util.Optional;

public interface SubjectService {
    List<Subject> findAll();
    List<Subject> findAllActive();
    Optional<Subject> findById(Long id);
    Subject save(Subject subject);
    void toggleActive(Long id);
    boolean existsByName(String name);
}
