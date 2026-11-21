package com.educenter.dto;

import java.math.BigDecimal;

public record StudentTopicProgress(String subject, String topic, int correctAnswers, int totalAnswers,
                                   int examCount, BigDecimal accuracyPercent, String interpretation) { }
