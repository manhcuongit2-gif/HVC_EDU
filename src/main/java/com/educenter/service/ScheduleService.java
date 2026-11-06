package com.educenter.service;

import com.educenter.dto.ScheduleDTO;
import com.educenter.entity.Schedule;

import java.time.LocalTime;
import java.util.List;

public interface ScheduleService {
    List<Schedule> getAllSchedules();
    List<Schedule> getSchedulesByClassId(Long classId);
    List<Schedule> getSchedulesByTeacherUserId(Long userId);
    List<Schedule> getSchedulesByDayOfWeek(Integer dayOfWeek);
    Schedule getScheduleById(Long id);
    Schedule saveSchedule(ScheduleDTO dto);
    Schedule updateSchedule(Long id, ScheduleDTO dto);
    void deleteSchedule(Long id);
    void checkScheduleConflicts(Long classId, Long roomId, Integer dayOfWeek, LocalTime start, LocalTime end, Long excludeId);
}
