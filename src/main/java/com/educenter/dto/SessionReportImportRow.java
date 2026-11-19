package com.educenter.dto;

public class SessionReportImportRow {
    private Long studentId;
    private String studentName;
    private String attendanceStatus;
    private String homeworkScore;
    private String comment;
    private String goodEvaluationCount;
    private String note;
    private int sourceRow;

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public String getHomeworkScore() { return homeworkScore; }
    public void setHomeworkScore(String homeworkScore) { this.homeworkScore = homeworkScore; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getGoodEvaluationCount() { return goodEvaluationCount; }
    public void setGoodEvaluationCount(String goodEvaluationCount) { this.goodEvaluationCount = goodEvaluationCount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public int getSourceRow() { return sourceRow; }
    public void setSourceRow(int sourceRow) { this.sourceRow = sourceRow; }
}
