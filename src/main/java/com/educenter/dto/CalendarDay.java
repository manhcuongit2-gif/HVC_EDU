package com.educenter.dto;

import com.educenter.entity.ClassSession;

import java.time.LocalDate;
import java.util.List;

public record CalendarDay(LocalDate date, List<ClassSession> sessions, boolean inMonth) {}
