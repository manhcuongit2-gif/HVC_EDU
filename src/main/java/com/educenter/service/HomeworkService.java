package com.educenter.service;

import com.educenter.dto.ParentHomeworkRow;
import com.educenter.entity.HomeworkAssignment;
import com.educenter.entity.HomeworkSubmission;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public interface HomeworkService {
    List<HomeworkAssignment> findForTeacher(Long teacherUserId);
    HomeworkAssignment createForTeacher(Long teacherUserId, Long classId, String title, String instructions, LocalDateTime dueAt, MultipartFile attachment);
    List<HomeworkSubmission> submissionsForTeacher(Long teacherUserId, Long assignmentId);
    HomeworkSubmission reviewForTeacher(Long teacherUserId, Long assignmentId, Long submissionId, BigDecimal score, String feedback);
    Path assignmentFileForTeacher(Long teacherUserId, Long assignmentId);
    Path submissionFileForTeacher(Long teacherUserId, Long assignmentId, Long submissionId);
    List<ParentHomeworkRow> findForParent(Long parentUserId);
    HomeworkSubmission submitForParent(Long parentUserId, Long assignmentId, Long studentId, MultipartFile file);
    Path assignmentFileForParent(Long parentUserId, Long assignmentId);
    Path submissionFileForParent(Long parentUserId, Long submissionId);
}
