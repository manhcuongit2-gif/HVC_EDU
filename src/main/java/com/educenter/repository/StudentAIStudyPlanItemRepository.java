package com.educenter.repository;

import com.educenter.entity.StudentAIStudyPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentAIStudyPlanItemRepository extends JpaRepository<StudentAIStudyPlanItem, Long> {
    List<StudentAIStudyPlanItem> findByPlanIdOrderByPositionAsc(Long planId);
    Optional<StudentAIStudyPlanItem> findByIdAndPlanStudentId(Long id, Long studentId);
    Optional<StudentAIStudyPlanItem> findByPlanIdAndItemKey(Long planId, String itemKey);
    void deleteByPlanIdAndIdNotIn(Long planId, List<Long> retainedIds);
    void deleteByPlanId(Long planId);
}
