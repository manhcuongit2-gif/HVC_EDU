package com.educenter.repository;

import com.educenter.entity.StudentAIStudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentAIStudyPlanRepository extends JpaRepository<StudentAIStudyPlan, Long> {
    Optional<StudentAIStudyPlan> findByStudentId(Long studentId);
}
