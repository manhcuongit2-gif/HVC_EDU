package com.educenter.repository;

import com.educenter.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findAllByOrderByDayOfWeekAscStartTimeAsc();

    List<Schedule> findByClassEntityId(Long classId);

    List<Schedule> findByRoomId(Long roomId);

    List<Schedule> findByDayOfWeekOrderByStartTimeAsc(Integer dayOfWeek);

    @Query("SELECT s FROM Schedule s WHERE s.classEntity.teacher.id = :teacherId AND s.active = true ORDER BY s.dayOfWeek ASC, s.startTime ASC")
    List<Schedule> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT s FROM Schedule s WHERE s.classEntity.teacher.user.id = :userId AND s.active = true ORDER BY s.dayOfWeek ASC, s.startTime ASC")
    List<Schedule> findByTeacherUserId(@Param("userId") Long userId);

    // Kiểm tra trùng lịch phòng học
    @Query("SELECT s FROM Schedule s WHERE s.room.id = :roomId AND s.dayOfWeek = :dayOfWeek AND s.active = true " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId) " +
           "AND (s.startTime < :endTime AND s.endTime > :startTime)")
    List<Schedule> findRoomConflicts(@Param("roomId") Long roomId,
                                     @Param("dayOfWeek") Integer dayOfWeek,
                                     @Param("startTime") LocalTime startTime,
                                     @Param("endTime") LocalTime endTime,
                                     @Param("excludeId") Long excludeId);

    // Kiểm tra trùng lịch giáo viên
    @Query("SELECT s FROM Schedule s WHERE s.classEntity.teacher.id = :teacherId AND s.dayOfWeek = :dayOfWeek AND s.active = true " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId) " +
           "AND (s.startTime < :endTime AND s.endTime > :startTime)")
    List<Schedule> findTeacherConflicts(@Param("teacherId") Long teacherId,
                                        @Param("dayOfWeek") Integer dayOfWeek,
                                        @Param("startTime") LocalTime startTime,
                                        @Param("endTime") LocalTime endTime,
                                        @Param("excludeId") Long excludeId);

    // Kiểm tra trùng lịch lớp học
    @Query("SELECT s FROM Schedule s WHERE s.classEntity.id = :classId AND s.dayOfWeek = :dayOfWeek AND s.active = true " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId) " +
           "AND (s.startTime < :endTime AND s.endTime > :startTime)")
    List<Schedule> findClassConflicts(@Param("classId") Long classId,
                                      @Param("dayOfWeek") Integer dayOfWeek,
                                      @Param("startTime") LocalTime startTime,
                                      @Param("endTime") LocalTime endTime,
                                      @Param("excludeId") Long excludeId);
}
