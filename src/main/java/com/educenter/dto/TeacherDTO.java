package com.educenter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TeacherDTO {

    private Long id;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 4, max = 50, message = "Tên đăng nhập từ 4 đến 50 ký tự")
    private String username;

    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @Pattern(regexp = "^(0[0-9]{9})?$", message = "Số điện thoại không hợp lệ (10 chữ số bắt đầu bằng 0)")
    private String phone;

    private String address;

    @NotBlank(message = "Chuyên môn giảng dạy không được để trống")
    private String specialization;

    private String degree;
    private String experience;
    private Boolean active = true;

    public TeacherDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
