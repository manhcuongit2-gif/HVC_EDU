package com.educenter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    @NotNull(message = "Vui lòng chọn lớp học")
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    @NotNull(message = "Vui lòng chọn phòng học")
    private Room room;

    @Column(name = "day_of_week", nullable = false)
    @NotNull(message = "Vui lòng chọn thứ trong tuần")
    @Min(value = 2, message = "Thứ trong tuần từ 2 (Thứ Hai) đến 8 (Chủ Nhật)")
    @Max(value = 8, message = "Thứ trong tuần từ 2 (Thứ Hai) đến 8 (Chủ Nhật)")
    private Integer dayOfWeek; // 2: Thứ 2, 3: Thứ 3, ..., 8: Chủ nhật

    @Column(name = "start_time", nullable = false)
    @NotNull(message = "Giờ bắt đầu không được để trống")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    @NotNull(message = "Giờ kết thúc không được để trống")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Schedule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getDayOfWeekDisplay() {
        if (dayOfWeek == null) return "";
        return switch (dayOfWeek) {
            case 2 -> "Thứ Hai";
            case 3 -> "Thứ Ba";
            case 4 -> "Thứ Tư";
            case 5 -> "Thứ Năm";
            case 6 -> "Thứ Sáu";
            case 7 -> "Thứ Bảy";
            case 8 -> "Chủ Nhật";
            default -> "Thứ " + dayOfWeek;
        };
    }

    public String getTimeRangeDisplay() {
        if (startTime == null || endTime == null) return "";
        return startTime + " - " + endTime;
    }
}
