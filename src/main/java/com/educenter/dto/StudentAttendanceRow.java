package com.educenter.dto;

import com.educenter.entity.ClassEntity;

import java.time.LocalDate;

public record StudentAttendanceRow(ClassEntity classEntity, LocalDate date, String status, String note) {
}
