package com.educenter.dto;

import com.educenter.entity.ExamAttempt;
import com.educenter.entity.ExamAttemptAnswer;
import java.util.List;

public record ExamAttemptReviewView(ExamAttempt attempt, List<ExamAttemptAnswer> answers) { }
