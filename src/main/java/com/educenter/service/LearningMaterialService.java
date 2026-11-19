package com.educenter.service;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.LearningMaterial;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface LearningMaterialService {
    List<LearningMaterial> findForParent(Long parentUserId, Long classId);
    List<ClassEntity> findParentClasses(Long parentUserId);
    List<LearningMaterial> findForTeacher(Long teacherUserId);
    List<ClassEntity> findTeacherClasses(Long teacherUserId);
    List<LearningMaterial> findAllForAdmin();
    LearningMaterial findForTeacherById(Long id, Long teacherUserId);
    LearningMaterial findForAdminById(Long id);
    LearningMaterial findFileForParent(Long id, Long parentUserId);
    LearningMaterial findFileForTeacher(Long id, Long teacherUserId);
    LearningMaterial findFileForAdmin(Long id);
    LearningMaterial createForTeacher(Long classId, String title, String description, String externalUrl,
                                      MultipartFile file, Long teacherUserId);
    LearningMaterial updateForTeacher(Long id, Long classId, String title, String description, String externalUrl,
                                      MultipartFile file, Long teacherUserId);
    void deleteForTeacher(Long id, Long teacherUserId);
    LearningMaterial createForAdmin(Long classId, String title, String description, String externalUrl,
                                    MultipartFile file, Long adminUserId);
    LearningMaterial updateForAdmin(Long id, Long classId, String title, String description, String externalUrl,
                                    MultipartFile file, Long adminUserId);
    void deleteForAdmin(Long id);
    Path resolveStoredFile(LearningMaterial material);
}
