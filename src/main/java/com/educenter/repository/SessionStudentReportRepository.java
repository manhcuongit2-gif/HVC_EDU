package com.educenter.repository;

import com.educenter.entity.SessionStudentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SessionStudentReportRepository extends JpaRepository<SessionStudentReport, Long> {
    List<SessionStudentReport> findByClassSessionIdOrderByStudentFullNameAsc(Long sessionId);
    List<SessionStudentReport> findByClassSessionId(Long sessionId);
    Optional<SessionStudentReport> findByClassSessionIdAndStudentId(Long sessionId, Long studentId);
    long countByClassSessionId(Long sessionId);

    @Query("select distinct r.classSession.id from SessionStudentReport r where r.classSession.classEntity.id = :classId")
    List<Long> findReportedSessionIdsForClass(@Param("classId") Long classId);

    @Query("select r from SessionStudentReport r join fetch r.classSession s join fetch s.classEntity c join fetch r.student st " +
            "where st.parent.user.id = :userId and s.publishedAt is not null order by s.sessionDate desc, s.startTime desc")
    List<SessionStudentReport> findPublishedForParent(@Param("userId") Long userId);

    @Query("select count(r) from SessionStudentReport r where r.classSession.classEntity.id = :classId and r.student.id = :studentId " +
            "and r.classSession.sessionDate between :from and :to and r.classSession.status = 'SCHEDULED' " +
            "and r.classSession.publishedAt is not null and r.attendanceStatus = 'PRESENT'")
    long countPublishedPresentForMonth(@Param("classId") Long classId, @Param("studentId") Long studentId,
                                       @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select r from SessionStudentReport r join fetch r.classSession s where r.student.id = :studentId " +
            "and s.classEntity.id = :classId and s.sessionDate between :from and :to and s.publishedAt is not null and s.status <> 'CANCELLED'")
    List<SessionStudentReport> findPublishedForStudentAndMonth(@Param("classId") Long classId, @Param("studentId") Long studentId,
                                                               @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select r from SessionStudentReport r join fetch r.classSession s join fetch s.classEntity c join fetch r.student st " +
            "where c.id = :classId and s.sessionDate between :from and :to and s.status <> 'CANCELLED' order by st.fullName, s.sessionDate")
    List<SessionStudentReport> findForClassAndMonth(@Param("classId") Long classId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
