package com.educenter.dto;

import com.educenter.entity.ClassSession;
import com.educenter.util.VietnameseDateFormatter;

import java.time.LocalDate;
import java.util.List;

public class CalendarDay {
    private final LocalDate date;
    private final List<ClassSession> sessions;
    private final boolean inMonth;

    public CalendarDay(LocalDate date, List<ClassSession> sessions, boolean inMonth) {
        this.date = date;
        this.sessions = sessions;
        this.inMonth = inMonth;
    }

    public LocalDate getDate() { return date; }
    public String getWeekdayDisplay() { return VietnameseDateFormatter.weekday(date); }
    public List<ClassSession> getSessions() { return sessions; }
    public boolean isInMonth() { return inMonth; }
}
