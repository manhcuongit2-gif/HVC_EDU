package com.educenter.service;

import com.educenter.dto.StudentPortalData;

public interface StudentPortalService {
    StudentPortalData getPortalData(Long studentUserId);
}
