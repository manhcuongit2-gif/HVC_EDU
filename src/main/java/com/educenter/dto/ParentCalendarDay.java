package com.educenter.dto;

import java.time.LocalDate;
import java.util.List;

public record ParentCalendarDay(LocalDate date, boolean currentMonth, boolean today,
                                List<ParentScheduleEvent> events) {}
