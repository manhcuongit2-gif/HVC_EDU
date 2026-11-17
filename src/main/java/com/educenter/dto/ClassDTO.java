package com.educenter.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ClassDTO {

    private Long id;

    @NotBlank(message = "Tên lớp học không được để trống")
    @Size(max = 100, message = "Tên lớp tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Vui lòng chọn môn học")
    private Long subjectId;

    @NotNull(message = "Vui lòng chọn giáo viên phụ trách")
    private Long teacherId;

    @NotNull(message = "Sĩ số tối đa không được để trống")
    @Min(value = 1, message = "Sĩ số tối thiểu là 1")
    private Integer maxStudents = 30;

    @NotNull(message = "Học phí không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Học phí không được âm")
    private BigDecimal tuitionFee;

    @NotNull(message = "Vui lòng nhập đơn giá mỗi buổi")
    @DecimalMin(value = "0.0", inclusive = true, message = "Đơn giá mỗi buổi không được âm")
    private BigDecimal sessionFee;

    private String status = "ACTIVE";

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    public ClassDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }

    public BigDecimal getTuitionFee() { return tuitionFee; }
    public void setTuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; }

    public BigDecimal getSessionFee() { return sessionFee; }
    public void setSessionFee(BigDecimal sessionFee) { this.sessionFee = sessionFee; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
