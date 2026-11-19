package com.educenter.service;

import com.educenter.dto.TeacherEarningsPage;
import com.educenter.entity.TeacherClassPayRate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface TeacherPayService {
    List<TeacherClassPayRate> getRateHistory(Long classId);
    void recordRate(Long adminUserId, Long classId, BigDecimal rate, LocalDate effectiveFrom);
    TeacherEarningsPage getEarnings(Long teacherUserId, YearMonth month);
}
