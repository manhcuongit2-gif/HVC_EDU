package com.educenter.repository;

import com.educenter.entity.HomeworkAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HomeworkAssignmentRepository extends JpaRepository<HomeworkAssignment, Long> {
    List<HomeworkAssignment> findByClassEntityIdInOrderByDueAtDesc(List<Long> classIds);
    List<HomeworkAssignment> findByClassEntityIdInAndPublishedAtLessThanEqualOrderByDueAtAsc(List<Long> classIds, LocalDateTime now);
    List<HomeworkAssignment> findByPublishedAtLessThanEqualAndDueAtBetweenOrderByDueAtAsc(LocalDateTime publishedAt, LocalDateTime from, LocalDateTime to);
    Optional<HomeworkAssignment> findByIdAndClassEntityIdIn(Long id, List<Long> classIds);
}
