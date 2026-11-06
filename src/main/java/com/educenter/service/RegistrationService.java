package com.educenter.service;

import com.educenter.entity.Registration;

import java.util.List;

public interface RegistrationService {
    List<Registration> getAllRegistrations();
    List<Registration> getPendingRegistrations();
    List<Registration> getRegistrationsByParentUserId(Long userId);
    Registration registerClass(Long parentUserId, Long studentId, Long classId, String note);
    void approveRegistration(Long registrationId, Long adminUserId, String adminNote);
    void rejectRegistration(Long registrationId, Long adminUserId, String adminNote);
    long getPendingCount();
}
