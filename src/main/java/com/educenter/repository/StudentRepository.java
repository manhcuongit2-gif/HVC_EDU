package com.educenter.repository;

import com.educenter.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByParentId(Long parentId);
    java.util.Optional<Student> findByFullNameAndParentId(String fullName, Long parentId);
    List<Student> findAllByOrderByIdAsc();
    List<Student> findByActiveTrue();
    List<Student> findByFullNameContainingIgnoreCase(String name);
}
