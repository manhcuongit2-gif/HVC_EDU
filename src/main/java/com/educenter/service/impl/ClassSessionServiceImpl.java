package com.educenter.service.impl;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassSession;
import com.educenter.entity.Schedule;
import com.educenter.entity.Room;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassSessionRepository;
import com.educenter.repository.ScheduleRepository;
import com.educenter.repository.RoomRepository;
import com.educenter.repository.SessionStudentReportRepository;
import com.educenter.service.ClassSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ClassSessionServiceImpl implements ClassSessionService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ClassSessionRepository sessionRepository;
    private final ClassRepository classRepository;
    private final ScheduleRepository scheduleRepository;
    private final RoomRepository roomRepository;
    private final SessionStudentReportRepository reportRepository;

    public ClassSessionServiceImpl(ClassSessionRepository sessionRepository,
                                   ClassRepository classRepository,
                                   ScheduleRepository scheduleRepository,
                                   RoomRepository roomRepository,
                                   SessionStudentReportRepository reportRepository) {
        this.sessionRepository = sessionRepository;
        this.classRepository = classRepository;
        this.scheduleRepository = scheduleRepository;
        this.roomRepository = roomRepository;
        this.reportRepository = reportRepository;
    }

    @Override
    @Transactional
    public void synchronizeAllClasses() {
        for (ClassEntity classEntity : classRepository.findAll()) {
            synchronizeClass(classEntity.getId());
        }
    }

    @Override
    @Transactional
    public void synchronizeClass(Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));
        List<ClassSession> existing = sessionRepository.findByClassEntityId(classId);
        Set<Long> reportedSessionIds = new HashSet<>(reportRepository.findReportedSessionIdsForClass(classId));
        if (classEntity.getStartDate() == null || classEntity.getEndDate() == null
                || classEntity.getStartDate().isAfter(classEntity.getEndDate())) {
            cancelFutureUnreported(existing);
            return;
        }

        List<Schedule> schedules = scheduleRepository.findByClassEntityId(classId);
        Set<Long> activeScheduleIds = new HashSet<>();
        for (Schedule schedule : schedules) {
            List<ClassSession> scheduleSessions = existing.stream()
                    .filter(session -> session.getSchedule() != null && Objects.equals(session.getSchedule().getId(), schedule.getId()))
                    .toList();
            if (Boolean.TRUE.equals(schedule.getActive())) {
                activeScheduleIds.add(schedule.getId());
                createExpectedSessions(classEntity, schedule, scheduleSessions, reportedSessionIds, scheduleSessions.isEmpty());
            }
        }

        LocalDate today = LocalDate.now(APP_ZONE);
        for (ClassSession session : existing) {
            if (session.getSchedule() == null || session.getOccurrenceDate() == null
                    || session.getOccurrenceDate().isBefore(today) || session.isManuallyAdjusted()
                    || session.getPublishedAt() != null || reportedSessionIds.contains(session.getId())) {
                continue;
            }
            Schedule source = schedules.stream()
                    .filter(schedule -> schedule.getId().equals(session.getSchedule().getId()))
                    .findFirst().orElse(null);
            if (source == null || !activeScheduleIds.contains(source.getId())
                    || !matchesWeekday(session.getOccurrenceDate(), source.getDayOfWeek())) {
                session.setStatus("CANCELLED");
                sessionRepository.save(session);
            } else {
                applyTemplate(session, classEntity, source);
                session.setStatus("SCHEDULED");
                sessionRepository.save(session);
            }
        }
    }

    private void createExpectedSessions(ClassEntity classEntity, Schedule schedule, List<ClassSession> existing,
                                        Set<Long> reportedSessionIds, boolean allowHistoricalBackfill) {
        if (schedule.getDayOfWeek() == null) return;
        Map<LocalDate, ClassSession> byOccurrenceDate = new HashMap<>();
        existing.forEach(session -> byOccurrenceDate.put(session.getOccurrenceDate(), session));
        LocalDate today = LocalDate.now(APP_ZONE);
        for (LocalDate date = classEntity.getStartDate(); !date.isAfter(classEntity.getEndDate()); date = date.plusDays(1)) {
            if (!matchesWeekday(date, schedule.getDayOfWeek())) continue;
            LocalDate occurrenceDate = date;
            ClassSession session = byOccurrenceDate.get(occurrenceDate);
            if (session == null && (allowHistoricalBackfill || !occurrenceDate.isBefore(today))) {
                session = new ClassSession();
                session.setSchedule(schedule);
                session.setOccurrenceDate(occurrenceDate);
                session.setSessionDate(occurrenceDate);
                session.setStatus("SCHEDULED");
                applyTemplate(session, classEntity, schedule);
                sessionRepository.save(session);
                byOccurrenceDate.put(occurrenceDate, session);
            } else if (session != null && !session.isManuallyAdjusted() && session.getPublishedAt() == null
                    && !reportedSessionIds.contains(session.getId()) && !occurrenceDate.isBefore(today)) {
                applyTemplate(session, classEntity, schedule);
                session.setStatus("SCHEDULED");
                sessionRepository.save(session);
            }
        }
    }

    private boolean matchesWeekday(LocalDate date, Integer scheduleDay) {
        return scheduleDay != null && date.getDayOfWeek().getValue() + 1 == scheduleDay;
    }

    private void applyTemplate(ClassSession session, ClassEntity classEntity, Schedule schedule) {
        session.setClassEntity(classEntity);
        session.setSchedule(schedule);
        session.setRoom(schedule.getRoom());
        session.setOccurrenceDate(session.getOccurrenceDate() == null ? session.getSessionDate() : session.getOccurrenceDate());
        session.setSessionDate(session.getOccurrenceDate());
        session.setStartTime(schedule.getStartTime());
        session.setEndTime(schedule.getEndTime());
    }

    private void cancelFutureUnreported(List<ClassSession> sessions) {
        if (sessions.isEmpty()) return;
        Set<Long> reportedSessionIds = new HashSet<>(reportRepository.findReportedSessionIdsForClass(sessions.get(0).getClassEntity().getId()));
        for (ClassSession session : sessions) {
            if (!session.getOccurrenceDate().isBefore(LocalDate.now(APP_ZONE)) && !session.isManuallyAdjusted()
                    && session.getPublishedAt() == null && !reportedSessionIds.contains(session.getId())) {
                session.setStatus("CANCELLED");
                sessionRepository.save(session);
            }
        }
    }

    @Override
    public List<ClassSession> findBetween(LocalDate from, LocalDate to, Long classId) {
        synchronizeAllClasses();
        return classId == null
                ? sessionRepository.findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(from, to)
                : sessionRepository.findByClassEntityIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(classId, from, to);
    }

    @Override
    public List<ClassSession> findForTeacher(Long userId, LocalDate from, LocalDate to) {
        synchronizeAllClasses();
        return sessionRepository.findForTeacherBetween(userId, from, to);
    }

    @Override
    public ClassSession getById(Long id) {
        return sessionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi học"));
    }

    @Override
    @Transactional
    public void cancel(Long sessionId) {
        ClassSession session = getById(sessionId);
        ensureAdjustable(session);
        session.setStatus("CANCELLED");
        session.setManuallyAdjusted(true);
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void reschedule(Long sessionId, LocalDate date, LocalTime startTime, LocalTime endTime, Long roomId) {
        ClassSession session = getById(sessionId);
        ensureAdjustable(session);
        if (date == null || startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Vui lòng nhập ngày và khung giờ hợp lệ cho buổi học.");
        }
        if (date.isBefore(LocalDate.now(APP_ZONE))) throw new IllegalArgumentException("Không thể chuyển buổi học sang ngày đã qua.");
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng học."));
        long conflicts = sessionRepository.countConflicts(date, startTime, endTime, room.getId(),
                session.getClassEntity().getTeacher().getId(), session.getClassEntity().getId(), sessionId);
        if (conflicts > 0) {
            throw new IllegalArgumentException("Buổi học mới bị trùng phòng, giáo viên hoặc lớp.");
        }
        session.setSessionDate(date);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setRoom(room);
        session.setStatus("SCHEDULED");
        session.setManuallyAdjusted(true);
        sessionRepository.save(session);
    }

    private void ensureAdjustable(ClassSession session) {
        if (session.getPublishedAt() != null || reportRepository.countByClassSessionId(session.getId()) > 0) {
            throw new IllegalArgumentException("Không thể sửa buổi đã có dữ liệu báo cáo.");
        }
        if (session.getSessionDate().isBefore(LocalDate.now(APP_ZONE))) {
            throw new IllegalArgumentException("Không thể hủy hoặc đổi lịch buổi đã qua.");
        }
    }

    @Transactional
    public void beforeScheduleDelete(Long scheduleId) {
        List<ClassSession> sessions = sessionRepository.findByScheduleId(scheduleId);
        Set<Long> reportedSessionIds = sessions.isEmpty() ? Set.of()
                : new HashSet<>(reportRepository.findReportedSessionIdsForClass(sessions.get(0).getClassEntity().getId()));
        for (ClassSession session : sessions) {
            if (!session.getOccurrenceDate().isBefore(LocalDate.now(APP_ZONE)) && session.getPublishedAt() == null
                    && !reportedSessionIds.contains(session.getId())) {
                session.setStatus("CANCELLED");
                session.setManuallyAdjusted(true);
            }
            session.setSchedule(null);
            sessionRepository.save(session);
        }
    }

}
