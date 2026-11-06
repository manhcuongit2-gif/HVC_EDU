package com.educenter.repository;

import com.educenter.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByName(String name);
    boolean existsByName(String name);
    List<Subject> findByActiveTrue();
    List<Subject> findAllByOrderByIdAsc();
}
