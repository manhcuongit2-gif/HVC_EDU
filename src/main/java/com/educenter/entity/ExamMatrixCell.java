package com.educenter.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "exam_matrix_cells")
public class ExamMatrixCell {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "blueprint_id", nullable = false) private ExamBlueprint blueprint;
    @Column(length = 150) private String topic;
    @Column(name = "question_type", nullable = false, length = 24) private String questionType;
    @Column(nullable = false, length = 20) private String difficulty;
    @Column(name = "question_count", nullable = false) private int questionCount;
    @Column(name = "cell_points", nullable = false, precision = 8, scale = 2) private BigDecimal cellPoints;
    public Long getId() { return id; }
    public ExamBlueprint getBlueprint() { return blueprint; }
    public void setBlueprint(ExamBlueprint value) { blueprint = value; }
    public String getTopic() { return topic; }
    public void setTopic(String value) { topic = value; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String value) { questionType = value; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String value) { difficulty = value; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int value) { questionCount = value; }
    public BigDecimal getCellPoints() { return cellPoints; }
    public void setCellPoints(BigDecimal value) { cellPoints = value; }
}
