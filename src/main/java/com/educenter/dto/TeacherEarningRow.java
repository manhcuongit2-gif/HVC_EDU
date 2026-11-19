package com.educenter.dto;

import com.educenter.entity.ClassSession;

import java.math.BigDecimal;

public record TeacherEarningRow(ClassSession session, BigDecimal rate, BigDecimal amount) {}
