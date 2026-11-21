package com.educenter.repository;

import com.educenter.entity.ExamBlueprintItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExamBlueprintItemRepository extends JpaRepository<ExamBlueprintItem, Long> {
    List<ExamBlueprintItem> findByBlueprintIdOrderByPositionAsc(Long blueprintId);
    Optional<ExamBlueprintItem> findByIdAndBlueprintId(Long id, Long blueprintId);
    boolean existsByBlueprintIdAndQuestionId(Long blueprintId, Long questionId);
}
