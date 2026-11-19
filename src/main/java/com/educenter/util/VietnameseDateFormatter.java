package com.educenter.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class VietnameseDateFormatter {
    private VietnameseDateFormatter() {}

    public static String weekday(LocalDate date) {
        if (date == null) return "";
        DayOfWeek day = date.getDayOfWeek();
        return switch (day) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ nhật";
        };
    }
}
