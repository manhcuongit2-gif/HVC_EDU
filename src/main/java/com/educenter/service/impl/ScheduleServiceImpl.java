package com.educenter.service.impl;

import com.educenter.dto.ScheduleDTO;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.Room;
import com.educenter.entity.Schedule;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.RoomRepository;
import com.educenter.repository.ScheduleRepository;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.service.ScheduleService;
import com.educenter.service.ClassSessionService;
import com.educenter.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ClassRepository classRepository;
    private final RoomRepository roomRepository;
    private final ClassSessionService classSessionService;
    private final ClassStudentRepository enrollments;
    private final NotificationService notificationService;

    public ScheduleServiceImpl(ScheduleRepository scheduleRepository,
                               ClassRepository classRepository,
                               RoomRepository roomRepository,
                               ClassSessionService classSessionService,
                               ClassStudentRepository enrollments,
                               NotificationService notificationService) {
        this.scheduleRepository = scheduleRepository;
        this.classRepository = classRepository;
        this.roomRepository = roomRepository;
        this.classSessionService = classSessionService;
        this.enrollments = enrollments;
        this.notificationService = notificationService;
    }

    @Override
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAllByOrderByDayOfWeekAscStartTimeAsc();
    }

    @Override
    public List<Schedule> getSchedulesByClassId(Long classId) {
        return scheduleRepository.findByClassEntityId(classId);
    }

    @Override
    public List<Schedule> getSchedulesByTeacherUserId(Long userId) {
        return scheduleRepository.findByTeacherUserId(userId);
    }

    @Override
    public List<Schedule> getSchedulesByDayOfWeek(Integer dayOfWeek) {
        return scheduleRepository.findByDayOfWeekOrderByStartTimeAsc(dayOfWeek);
    }

    @Override
    public Schedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch học với ID: " + id));
    }

    @Override
    public void checkScheduleConflicts(Long classId, Long roomId, Integer dayOfWeek, LocalTime start, LocalTime end, Long excludeId) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Giờ bắt đầu (" + start + ") phải trước giờ kết thúc (" + end + ")");
        }

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng học"));

        // 1. Kiểm tra trùng phòng học
        List<Schedule> roomConflicts = scheduleRepository.findRoomConflicts(roomId, dayOfWeek, start, end, excludeId);
        if (!roomConflicts.isEmpty()) {
            Schedule c = roomConflicts.get(0);
            throw new IllegalArgumentException(String.format(
                    "TRÙNG PHÒNG HỌC: %s đã có lớp '%s' sử dụng vào %s (%s - %s)!",
                    room.getName(), c.getClassEntity().getName(), c.getDayOfWeekDisplay(), c.getStartTime(), c.getEndTime()
            ));
        }

        // 2. Kiểm tra trùng lịch người dạy
        Long teacherId = classEntity.getTeacher().getId();
        List<Schedule> teacherConflicts = scheduleRepository.findTeacherConflicts(teacherId, dayOfWeek, start, end, excludeId);
        if (!teacherConflicts.isEmpty()) {
            Schedule c = teacherConflicts.get(0);
            throw new IllegalArgumentException(String.format(
                    "TRÙNG LỊCH NGƯỜI DẠY: Người dạy '%s' đã có lịch dạy lớp '%s' vào %s (%s - %s)!",
                    classEntity.getTeacher().getUser().getFullName(), c.getClassEntity().getName(), c.getDayOfWeekDisplay(), c.getStartTime(), c.getEndTime()
            ));
        }

        // 3. Kiểm tra trùng lịch của chính lớp học này
        List<Schedule> classConflicts = scheduleRepository.findClassConflicts(classId, dayOfWeek, start, end, excludeId);
        if (!classConflicts.isEmpty()) {
            Schedule c = classConflicts.get(0);
            throw new IllegalArgumentException(String.format(
                    "TRÙNG LỊCH LỚP HỌC: Lớp '%s' đã có lịch học vào %s (%s - %s)!",
                    classEntity.getName(), c.getDayOfWeekDisplay(), c.getStartTime(), c.getEndTime()
            ));
        }
    }

    @Override
    @Transactional
    public Schedule saveSchedule(ScheduleDTO dto) {
        checkScheduleConflicts(dto.getClassId(), dto.getRoomId(), dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime(), null);

        ClassEntity classEntity = classRepository.findById(dto.getClassId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));
        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng học"));

        Schedule s = new Schedule();
        s.setClassEntity(classEntity);
        s.setRoom(room);
        s.setDayOfWeek(dto.getDayOfWeek());
        s.setStartTime(dto.getStartTime());
        s.setEndTime(dto.getEndTime());
        s.setActive(dto.getActive() != null ? dto.getActive() : true);

        Schedule saved = scheduleRepository.save(s);
        classSessionService.synchronizeClass(classEntity.getId());
        notifyParents(saved, "schedule-created-" + saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Schedule updateSchedule(Long id, ScheduleDTO dto) {
        Schedule s = getScheduleById(id);
        ClassEntity oldClass = s.getClassEntity();

        checkScheduleConflicts(dto.getClassId(), dto.getRoomId(), dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime(), id);

        ClassEntity classEntity = classRepository.findById(dto.getClassId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));
        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng học"));

        s.setClassEntity(classEntity);
        s.setRoom(room);
        s.setDayOfWeek(dto.getDayOfWeek());
        s.setStartTime(dto.getStartTime());
        s.setEndTime(dto.getEndTime());
        s.setActive(dto.getActive());

        Schedule saved = scheduleRepository.save(s);
        classSessionService.synchronizeClass(classEntity.getId());
        if (!oldClass.getId().equals(classEntity.getId())) {
            notificationService.notifyUsers(enrollments.findByClassEntityIdAndStatus(oldClass.getId(), "ACTIVE").stream()
                            .map(e -> e.getStudent().getParent()).filter(java.util.Objects::nonNull).map(p -> p.getUser())
                            .filter(java.util.Objects::nonNull).map(u -> u.getId()).distinct().toList(),
                    "schedule-moved-away-" + saved.getId() + "-" + classEntity.getId(), "SCHEDULE_CHANGE",
                    "Lịch học lớp " + oldClass.getName() + " đã được chuyển", "Giáo viên đã chuyển lịch sang lớp " + classEntity.getName() + ".", "/parent/schedule");
        }
        notifyParents(saved, "schedule-updated-" + saved.getId() + "-" + saved.getDayOfWeek() + "-" + saved.getStartTime() + "-" + saved.getEndTime() + "-" + saved.getRoom().getId());
        return saved;
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        Schedule schedule = getScheduleById(id);
        classSessionService.beforeScheduleDelete(id);
        scheduleRepository.deleteById(id);
        classSessionService.synchronizeClass(schedule.getClassEntity().getId());
        notificationService.notifyUsers(enrollments.findByClassEntityIdAndStatus(schedule.getClassEntity().getId(), "ACTIVE").stream()
                .map(e -> e.getStudent().getParent()).filter(java.util.Objects::nonNull).map(p -> p.getUser())
                .filter(java.util.Objects::nonNull).map(u -> u.getId()).distinct().toList(),
                "schedule-deleted-" + id, "SCHEDULE_CHANGE", "Lịch học đã được cập nhật",
                "Lịch học lớp " + schedule.getClassEntity().getName() + " đã bị xóa.", "/parent/schedule");
    }

    private void notifyParents(Schedule schedule, String eventKey) {
        ClassEntity classEntity = schedule.getClassEntity();
        String body = classEntity.getName() + " · " + schedule.getDayOfWeekDisplay() + " · " + schedule.getTimeRangeDisplay()
                + (schedule.getRoom() == null ? "" : " · Phòng " + schedule.getRoom().getName());
        notificationService.notifyUsers(enrollments.findByClassEntityIdAndStatus(classEntity.getId(), "ACTIVE").stream()
                        .map(e -> e.getStudent().getParent()).filter(java.util.Objects::nonNull).map(p -> p.getUser())
                        .filter(java.util.Objects::nonNull).map(u -> u.getId()).distinct().toList(),
                eventKey, "SCHEDULE_CHANGE", "Lịch học lớp " + classEntity.getName() + " đã thay đổi", body, "/parent/schedule");
    }
}
