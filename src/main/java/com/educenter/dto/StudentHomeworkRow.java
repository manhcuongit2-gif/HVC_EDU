package com.educenter.dto;

import com.educenter.entity.HomeworkAssignment;
import com.educenter.entity.HomeworkSubmission;

public record StudentHomeworkRow(HomeworkAssignment assignment, HomeworkSubmission submission, String status) {
}
