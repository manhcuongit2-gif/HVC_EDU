package com.educenter.service.impl;

/** Content safe to display on a device lock screen. Never includes business details. */
final class PushNotificationContent {
    private PushNotificationContent() {}

    static String title(String category) {
        if (category == null) return "Thông báo mới";
        return switch (category) {
            case "MESSAGE_PRIVATE", "MESSAGE_GROUP" -> "Tin nhắn mới";
            case "TUITION", "TUITION_REMINDER" -> "Cập nhật học phí";
            case "SCHEDULE_REMINDER", "SCHEDULE_CHANGE" -> "Cập nhật lịch học";
            case "ATTENDANCE" -> "Cập nhật điểm danh";
            case "LEARNING_REPORT" -> "Cập nhật học tập";
            case "HOMEWORK" -> "Cập nhật bài tập";
            default -> "Thông báo mới";
        };
    }

    static String body(String category) {
        return category != null && category.startsWith("MESSAGE")
                ? "Bạn có tin nhắn mới."
                : "Bạn có thông báo mới.";
    }
}
