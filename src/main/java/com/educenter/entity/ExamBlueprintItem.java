package com.educenter.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "exam_blueprint_items")
public class ExamBlueprintItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "blueprint_id", nullable = false) private ExamBlueprint blueprint;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "matrix_cell_id", nullable = false) private ExamMatrixCell matrixCell;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "question_id", nullable = false) private ExamQuestion question;
    @Column(nullable = false, precision = 8, scale = 2) private BigDecimal points;
    @Column(nullable = false) private int position;
    public Long getId() { return id; }
    public ExamBlueprint getBlueprint() { return blueprint; }
    public void setBlueprint(ExamBlueprint value) { blueprint = value; }
    public ExamMatrixCell getMatrixCell() { return matrixCell; }
    public void setMatrixCell(ExamMatrixCell value) { matrixCell = value; }
    public ExamQuestion getQuestion() { return question; }
    public void setQuestion(ExamQuestion value) { question = value; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal value) { points = value; }
    public int getPosition() { return position; }
    public void setPosition(int value) { position = value; }
}
