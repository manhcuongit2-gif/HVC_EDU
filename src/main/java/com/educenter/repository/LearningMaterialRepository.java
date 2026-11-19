package com.educenter.repository;

import com.educenter.entity.LearningMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long> {
    List<LearningMaterial> findAllByOrderByCreatedAtDesc();
    List<LearningMaterial> findByClassEntityIdOrderByCreatedAtDesc(Long classId);
    List<LearningMaterial> findByClassEntityIdInOrderByCreatedAtDesc(List<Long> classIds);
}
