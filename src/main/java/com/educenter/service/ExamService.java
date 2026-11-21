package com.educenter.service;

import com.educenter.dto.*;
import com.educenter.entity.*;
import java.math.BigDecimal;
import java.util.List;

public interface ExamService {
    List<ExamQuestion> questionsForTeacher(Long teacherUserId);
    List<ExamQuestion> searchQuestions(Long teacherUserId, Long subjectId, String gradeLevel, String topic, String questionType, String difficulty);
    ExamQuestionForm questionFormForTeacher(Long teacherUserId, Long questionId);
    ExamQuestion saveQuestion(Long teacherUserId, ExamQuestionForm form);
    void archiveQuestion(Long teacherUserId, Long questionId);
    List<Subject> subjects();
    List<ExamBlueprint> blueprintsForTeacher(Long teacherUserId);
    ExamBlueprint createBlueprint(Long teacherUserId, ExamBlueprintForm form);
    ExamBlueprint saveImportedBlueprint(Long teacherUserId, ExamImportSaveForm form);
    ExamBlueprint blueprintForTeacher(Long teacherUserId, Long blueprintId);
    List<ExamBlueprintItem> itemsForTeacher(Long teacherUserId, Long blueprintId);
    void updateItemPoints(Long teacherUserId, Long blueprintId, java.util.Map<Long, BigDecimal> points);
    void replaceItem(Long teacherUserId, Long blueprintId, Long itemId);
    List<ClassEntity> classesForTeacher(Long teacherUserId);
    ExamAssignment assign(Long teacherUserId, Long blueprintId, ExamAssignmentForm form);
    ExamAssignment createDirectAssignment(Long teacherUserId, DirectExamForm form);
    List<ExamAssignment> assignmentsForTeacher(Long teacherUserId);
    ExamAssignment assignmentForTeacher(Long teacherUserId, Long assignmentId);
    List<TeacherExamAttemptView> attemptsForTeacher(Long teacherUserId, Long assignmentId);
    TeacherExamAttemptView attemptForTeacher(Long teacherUserId, Long assignmentId, Long attemptId);
    void gradeEssay(Long teacherUserId, Long assignmentId, Long attemptId, int questionNumber, BigDecimal score, String feedback);
    void publishResults(Long teacherUserId, Long assignmentId);
    ExamAssignmentReport reportForTeacher(Long teacherUserId, Long assignmentId);
    List<StudentExamAssignmentView> assignmentsForStudent(Long studentUserId);
    StudentExamAttemptView startOrResume(Long studentUserId, Long assignmentId);
    StudentExamAttemptView attemptForStudent(Long studentUserId, Long attemptId);
    boolean saveAnswer(Long studentUserId, Long attemptId, int questionNumber, String answerText, String optionKey);
    void submitAttempt(Long studentUserId, Long attemptId);
    List<StudentExamAssignmentView> publishedResultsForStudent(Long studentUserId);
    List<StudentExamAssignmentView> publishedResultsForParent(Long parentUserId, Long childId);
    ExamExportDocument exportForTeacher(Long teacherUserId, Long assignmentId, boolean answerKey);
    void autoSubmitExpired();
    void notifyUpcomingDeadlines();
}
