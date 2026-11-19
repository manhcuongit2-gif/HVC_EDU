package com.educenter.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record TeacherEarningsPage(List<TeacherEarningRow> sessions,
                                 BigDecimal totalEarned,
                                 BigDecimal monthEarned,
                                 YearMonth month) {}
