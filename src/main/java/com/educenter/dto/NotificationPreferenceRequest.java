package com.educenter.dto;

import java.time.LocalTime;

public record NotificationPreferenceRequest(boolean pushEnabled, boolean privateMessages, boolean groupMessages,
                                            boolean tuition, boolean tuitionReminders, boolean scheduleReminders,
                                            boolean scheduleChanges, boolean attendance, boolean learningReports,
                                            boolean homework, boolean general, boolean soundEnabled,
                                            boolean previewEnabled, boolean quietHoursEnabled,
                                            LocalTime quietStart, LocalTime quietEnd) { }
