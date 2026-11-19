package com.educenter.repository;

import com.educenter.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    List<ClassSession> findByScheduleId(Long scheduleId);
    List<ClassSession> findByClassEntityId(Long classId);
    Optional<ClassSession> findByScheduleIdAndOccurrenceDate(Long scheduleId, LocalDate occurrenceDate);
    List<ClassSession> findByClassEntityIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(Long classId, LocalDate from, LocalDate to);
    List<ClassSession> findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(LocalDate from, LocalDate to);
    Optional<ClassSession> findFirstByClassEntityIdAndSessionDateGreaterThanEqualAndStatusNotOrderBySessionDateAscStartTimeAsc(
            Long classId, LocalDate from, String excludedStatus);

    @Query("select s from ClassSession s where s.classEntity.id = :classId and s.status <> 'CANCELLED' " +
            "and (s.sessionDate > :today or (s.sessionDate = :today and s.startTime >= :time)) " +
            "order by s.sessionDate, s.startTime")
    List<ClassSession> findUpcomingForClass(@Param("classId") Long classId, @Param("today") LocalDate today,
                                            @Param("time") java.time.LocalTime time);

    @Query("select s from ClassSession s where s.classEntity.teacher.user.id = :userId and s.sessionDate between :from and :to order by s.sessionDate, s.startTime")
    List<ClassSession> findForTeacherBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select distinct s from ClassSession s where s.classEntity.teacher.user.id = :userId " +
            "and (s.sessionDate < :today or (s.sessionDate = :today and s.endTime <= :throughTime)) " +
            "and s.status <> 'CANCELLED' " +
            "and exists (select r.id from SessionStudentReport r where r.classSession.id = s.id) " +
            "order by s.sessionDate desc, s.startTime desc")
    List<ClassSession> findEarningSessionsForTeacher(@Param("userId") Long userId,
                                                    @Param("today") LocalDate today,
                                                    @Param("throughTime") LocalTime throughTime);

    @Query("select s from ClassSession s where s.classEntity.id = :classId and s.sessionDate between :from and :to and s.status <> 'CANCELLED' order by s.sessionDate, s.startTime")
    List<ClassSession> findReportableForClassBetween(@Param("classId") Long classId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select s from ClassSession s where s.classEntity.id = :classId and s.sessionDate between :from and :to and s.status <> 'CANCELLED' and s.publishedAt is null order by s.sessionDate, s.startTime")
    List<ClassSession> findUnpublishedForClassBetween(@Param("classId") Long classId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select s from ClassSession s where s.sessionDate between :from and :to and s.status <> 'CANCELLED' order by s.sessionDate, s.startTime")
    List<ClassSession> findAllReportableBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select count(s) from ClassSession s where s.sessionDate = :date and s.status <> 'CANCELLED' and s.id <> :excludeId " +
            "and s.startTime < :endTime and s.endTime > :startTime and " +
            "(s.room.id = :roomId or s.classEntity.teacher.id = :teacherId or s.classEntity.id = :classId)")
    long countConflicts(@Param("date") LocalDate date, @Param("startTime") java.time.LocalTime startTime,
                        @Param("endTime") java.time.LocalTime endTime, @Param("roomId") Long roomId,
                        @Param("teacherId") Long teacherId, @Param("classId") Long classId, @Param("excludeId") Long excludeId);
}
