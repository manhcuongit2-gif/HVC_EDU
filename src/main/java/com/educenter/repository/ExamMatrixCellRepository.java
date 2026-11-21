package com.educenter.repository;

import com.educenter.entity.ExamMatrixCell;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamMatrixCellRepository extends JpaRepository<ExamMatrixCell, Long> {
    List<ExamMatrixCell> findByBlueprintIdOrderByIdAsc(Long blueprintId);
}
