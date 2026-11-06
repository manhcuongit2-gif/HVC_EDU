package com.educenter.repository;

import com.educenter.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByClassEntityIdAndAttendanceDate(Long classId, LocalDate date);
    List<Attendance> findByClassEntityId(Long classId);
    List<Attendance> findByStudentIdOrderByAttendanceDateDesc(Long studentId);
    Optional<Attendance> findByClassEntityIdAndStudentIdAndAttendanceDate(Long classId, Long studentId, LocalDate date);
}
