package com.educenter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

public class ScheduleDTO {

    private Long id;

    @NotNull(message = "Vui lòng chọn lớp học")
    private Long classId;

    @NotNull(message = "Vui lòng chọn phòng học")
    private Long roomId;

    @NotNull(message = "Vui lòng chọn thứ trong tuần")
    @Min(value = 2, message = "Thứ trong tuần không hợp lệ (2 - 8)")
    @Max(value = 8, message = "Thứ trong tuần không hợp lệ (2 - 8)")
    private Integer dayOfWeek;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Boolean active = true;

    public ScheduleDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
