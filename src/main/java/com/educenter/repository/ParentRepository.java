package com.educenter.repository;

import com.educenter.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {
    Optional<Parent> findByUserId(Long userId);
    List<Parent> findAllByOrderByIdAsc();
    List<Parent> findByUserActiveTrue();
}
