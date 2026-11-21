package com.educenter.dto;

import java.time.LocalDate;
import java.util.List;

public record StudentCalendarDay(LocalDate date, boolean currentMonth, boolean today,
                                 List<StudentScheduleEvent> events) {
}
