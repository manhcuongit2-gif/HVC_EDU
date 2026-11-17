package com.educenter.repository;

import com.educenter.entity.MonthlyStudentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MonthlyStudentReportRepository extends JpaRepository<MonthlyStudentReport, Long> {
    Optional<MonthlyStudentReport> findByClassEntityIdAndStudentIdAndReportMonth(Long classId, Long studentId, LocalDate reportMonth);
    List<MonthlyStudentReport> findByClassEntityIdAndReportMonthOrderByStudentFullNameAsc(Long classId, LocalDate reportMonth);

    @Query("select r from MonthlyStudentReport r join fetch r.classEntity c join fetch r.student s " +
            "where r.reportMonth = :month and r.publishedAt is not null order by c.name, s.fullName")
    List<MonthlyStudentReport> findAllPublishedForMonth(@Param("month") LocalDate month);

    @Query("select r from MonthlyStudentReport r join fetch r.classEntity c join fetch r.student s " +
            "where c.id = :classId and r.reportMonth = :month and r.publishedAt is not null order by s.fullName")
    List<MonthlyStudentReport> findPublishedForClassAndMonth(@Param("classId") Long classId, @Param("month") LocalDate month);

    @Query("select r from MonthlyStudentReport r join fetch r.classEntity c join fetch r.student s " +
            "where s.parent.user.id = :userId and r.publishedAt is not null order by r.reportMonth desc, c.name, s.fullName")
    List<MonthlyStudentReport> findPublishedForParent(@Param("userId") Long userId);
}
