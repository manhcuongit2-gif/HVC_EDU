package com.educenter.repository;

import com.educenter.entity.TeacherClassPayRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherClassPayRateRepository extends JpaRepository<TeacherClassPayRate, Long> {
    List<TeacherClassPayRate> findByClassEntityIdOrderByEffectiveFromDesc(Long classId);
    List<TeacherClassPayRate> findByClassEntityIdInOrderByEffectiveFromDesc(List<Long> classIds);
    Optional<TeacherClassPayRate> findFirstByClassEntityIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(Long classId, LocalDate date);
    Optional<TeacherClassPayRate> findByClassEntityIdAndEffectiveFrom(Long classId, LocalDate effectiveFrom);
}
