package com.educenter.entity;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "class_students", uniqueConstraints = {
    @UniqueConstraint(name = "uk_class_student", columnNames = {"class_id", "student_id"})
})
public class ClassStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "enrolled_date", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate enrolledDate = LocalDate.now();

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE"; // ACTIVE, DROPPED

    public ClassStudent() {}

    public ClassStudent(ClassEntity classEntity, Student student, LocalDate enrolledDate, String status) {
        this.classEntity = classEntity;
        this.student = student;
        this.enrolledDate = enrolledDate != null ? enrolledDate : LocalDate.now();
        this.status = status != null ? status : "ACTIVE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public LocalDate getEnrolledDate() { return enrolledDate; }
    public void setEnrolledDate(LocalDate enrolledDate) { this.enrolledDate = enrolledDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
