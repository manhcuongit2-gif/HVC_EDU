package com.educenter.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ExamScoring {
    private ExamScoring() { }

    public static boolean exactFillMatch(String response, List<String> acceptedAnswers) {
        if (response == null || acceptedAnswers == null) return false;
        String normalized = normalize(response);
        return !normalized.isEmpty() && acceptedAnswers.stream().filter(value -> value != null)
                .map(ExamScoring::normalize).anyMatch(normalized::equals);
    }

    public static List<BigDecimal> dividePoints(BigDecimal total, int count) {
        if (total == null || total.signum() <= 0 || count <= 0) throw new IllegalArgumentException("Tổng điểm và số câu phải lớn hơn 0.");
        long cents = total.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        long base = cents / count, remainder = cents % count;
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < count; i++) values.add(BigDecimal.valueOf(base + (i < remainder ? 1 : 0), 2));
        return List.copyOf(values);
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
