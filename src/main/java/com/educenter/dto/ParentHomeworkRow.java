package com.educenter.dto;

import com.educenter.entity.HomeworkAssignment;
import com.educenter.entity.HomeworkSubmission;
import com.educenter.entity.Student;

public record ParentHomeworkRow(HomeworkAssignment assignment, Student student, HomeworkSubmission submission) { }
