package com.educenter.service.impl;

import com.educenter.entity.Subject;
import com.educenter.repository.SubjectRepository;
import com.educenter.service.SubjectService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectServiceImpl(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Override
    public List<Subject> findAll() {
        return subjectRepository.findAllByOrderByIdAsc();
    }

    @Override
    public List<Subject> findAllActive() {
        return subjectRepository.findByActiveTrue();
    }

    @Override
    public Optional<Subject> findById(Long id) {
        return subjectRepository.findById(id);
    }

    @Override
    public Subject save(Subject subject) {
        return subjectRepository.save(subject);
    }

    @Override
    public void toggleActive(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học với ID: " + id));
        subject.setActive(!subject.getActive());
        subjectRepository.save(subject);
    }

    @Override
    public boolean existsByName(String name) {
        return subjectRepository.existsByName(name);
    }
}
