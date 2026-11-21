package com.educenter.repository;

import com.educenter.entity.ExamBlueprint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExamBlueprintRepository extends JpaRepository<ExamBlueprint, Long> {
    List<ExamBlueprint> findByOwnerTeacherIdOrderByCreatedAtDesc(Long ownerTeacherId);
    Optional<ExamBlueprint> findByIdAndOwnerTeacherId(Long id, Long ownerTeacherId);
}
