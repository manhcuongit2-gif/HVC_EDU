package com.educenter.service.impl;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassStudent;
import com.educenter.entity.LearningMaterial;
import com.educenter.entity.User;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.repository.LearningMaterialRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.LearningMaterialService;
import com.educenter.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class LearningMaterialServiceImpl implements LearningMaterialService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"), Map.entry("gif", "image/gif"), Map.entry("webp", "image/webp")
    );

    private final LearningMaterialRepository materialRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Path uploadDirectory;

    public LearningMaterialServiceImpl(LearningMaterialRepository materialRepository,
                                       ClassRepository classRepository,
                                       ClassStudentRepository classStudentRepository,
                                       UserRepository userRepository,
                                       NotificationService notificationService,
                                       @Value("${app.storage.upload-dir:./uploads}") String uploadDirectory) {
        this.materialRepository = materialRepository;
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public List<LearningMaterial> findForParent(Long parentUserId, Long classId) {
        List<ClassEntity> classes = findParentClasses(parentUserId);
        Set<Long> classIds = new HashSet<>();
        classes.forEach(c -> classIds.add(c.getId()));
        if (classId != null && !classIds.contains(classId)) {
            throw new IllegalArgumentException("Bạn không có quyền xem tài liệu của lớp này.");
        }
        if (classId != null) return materialRepository.findByClassEntityIdOrderByCreatedAtDesc(classId);
        if (classIds.isEmpty()) return List.of();
        return materialRepository.findByClassEntityIdInOrderByCreatedAtDesc(new ArrayList<>(classIds));
    }

    @Override
    public List<ClassEntity> findParentClasses(Long parentUserId) {
        Map<Long, ClassEntity> classes = new LinkedHashMap<>();
        for (ClassStudent enrollment : classStudentRepository.findByStudentParentUserId(parentUserId)) {
            if ("ACTIVE".equals(enrollment.getStatus())) {
                classes.putIfAbsent(enrollment.getClassEntity().getId(), enrollment.getClassEntity());
            }
        }
        return new ArrayList<>(classes.values());
    }

    @Override
    public List<LearningMaterial> findForTeacher(Long teacherUserId) {
        List<Long> ids = findTeacherClasses(teacherUserId).stream().map(ClassEntity::getId).toList();
        return ids.isEmpty() ? List.of() : materialRepository.findByClassEntityIdInOrderByCreatedAtDesc(ids);
    }

    @Override
    public List<ClassEntity> findTeacherClasses(Long teacherUserId) {
        return classRepository.findByTeacherUserId(teacherUserId);
    }

    @Override
    public List<LearningMaterial> findAllForAdmin() {
        return materialRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public LearningMaterial findForTeacherById(Long id, Long teacherUserId) {
        LearningMaterial material = getMaterial(id);
        requireTeacherOwns(material.getClassEntity(), teacherUserId);
        return material;
    }

    @Override
    public LearningMaterial findForAdminById(Long id) {
        return getMaterial(id);
    }

    @Override
    public LearningMaterial findFileForParent(Long id, Long parentUserId) {
        LearningMaterial material = getMaterial(id);
        if (findParentClasses(parentUserId).stream().noneMatch(c -> c.getId().equals(material.getClassEntity().getId()))) {
            throw new IllegalArgumentException("Bạn không có quyền tải tài liệu này.");
        }
        requireFile(material);
        return material;
    }

    @Override
    public LearningMaterial findFileForTeacher(Long id, Long teacherUserId) {
        LearningMaterial material = findForTeacherById(id, teacherUserId);
        requireFile(material);
        return material;
    }

    @Override
    public LearningMaterial findFileForAdmin(Long id) {
        LearningMaterial material = getMaterial(id);
        requireFile(material);
        return material;
    }

    @Override
    @Transactional
    public LearningMaterial createForTeacher(Long classId, String title, String description, String externalUrl,
                                             MultipartFile file, Long teacherUserId) {
        return save(null, classId, title, description, externalUrl, file, teacherUserId, false);
    }

    @Override
    @Transactional
    public LearningMaterial updateForTeacher(Long id, Long classId, String title, String description, String externalUrl,
                                             MultipartFile file, Long teacherUserId) {
        return save(id, classId, title, description, externalUrl, file, teacherUserId, false);
    }

    @Override
    @Transactional
    public void deleteForTeacher(Long id, Long teacherUserId) {
        delete(findForTeacherById(id, teacherUserId));
    }

    @Override
    @Transactional
    public LearningMaterial createForAdmin(Long classId, String title, String description, String externalUrl,
                                           MultipartFile file, Long adminUserId) {
        return save(null, classId, title, description, externalUrl, file, adminUserId, true);
    }

    @Override
    @Transactional
    public LearningMaterial updateForAdmin(Long id, Long classId, String title, String description, String externalUrl,
                                           MultipartFile file, Long adminUserId) {
        return save(id, classId, title, description, externalUrl, file, adminUserId, true);
    }

    @Override
    @Transactional
    public void deleteForAdmin(Long id) {
        delete(getMaterial(id));
    }

    @Override
    public Path resolveStoredFile(LearningMaterial material) {
        if (material.getStoredFilename() == null || material.getStoredFilename().isBlank()) {
            throw new IllegalArgumentException("Tài liệu này không có tệp tải lên.");
        }
        Path path = uploadDirectory.resolve(material.getStoredFilename()).normalize();
        if (!path.startsWith(uploadDirectory)) throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ.");
        return path;
    }

    private LearningMaterial save(Long id, Long classId, String title, String description, String externalUrl,
                                  MultipartFile file, Long actorUserId, boolean admin) {
        if (title == null || title.isBlank() || title.trim().length() > 150) {
            throw new IllegalArgumentException("Tên tài liệu là bắt buộc và tối đa 150 ký tự.");
        }
        String normalizedUrl = normalizeUrl(externalUrl);
        boolean hasFile = file != null && !file.isEmpty();

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học."));
        if (!admin) requireTeacherOwns(classEntity, actorUserId);

        LearningMaterial material = id == null ? new LearningMaterial() : getMaterial(id);
        if (id != null && !admin) requireTeacherOwns(material.getClassEntity(), actorUserId);
        if (!hasFile && normalizedUrl == null && material.getStoredFilename() == null) {
            throw new IllegalArgumentException("Vui lòng tải tệp lên hoặc nhập liên kết tài liệu.");
        }
        String oldStoredFilename = material.getStoredFilename();
        material.setClassEntity(classEntity);
        material.setTitle(title.trim());
        material.setDescription(description == null || description.isBlank() ? null : description.trim());
        material.setExternalUrl(normalizedUrl);
        material.setCreatedBy(id == null ? userRepository.findById(actorUserId).orElse(null) : material.getCreatedBy());

        String newStoredFilename = null;
        if (hasFile) {
            validateFile(file);
            newStoredFilename = store(file);
            material.setStoredFilename(newStoredFilename);
            material.setOriginalFilename(cleanOriginalFilename(file.getOriginalFilename()));
            material.setContentType(contentTypeFor(file.getOriginalFilename()));
            material.setFileSize(file.getSize());
            material.setAiIndexStatus("PENDING");
        }

        LearningMaterial saved = materialRepository.save(material);
        if (id == null) {
            List<Long> parents = classStudentRepository.findByClassEntityIdAndStatus(classEntity.getId(), "ACTIVE").stream()
                    .map(ClassStudent::getStudent).filter(Objects::nonNull).map(s -> s.getParent()).filter(Objects::nonNull)
                    .map(p -> p.getUser()).filter(Objects::nonNull).map(User::getId).distinct().toList();
            notificationService.notifyUsers(parents, "learning-material-" + saved.getId(), "GENERAL", "Tài liệu mới cho lớp " + classEntity.getName(),
                    "Giáo viên đã đăng tài liệu: " + saved.getTitle() + ".", "/parent/materials");
        }
        if (newStoredFilename != null && oldStoredFilename != null && !oldStoredFilename.equals(newStoredFilename)) {
            deleteStoredFile(oldStoredFilename);
        }
        return saved;
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Mỗi tệp không được vượt quá 20 MB.");
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_TYPES.containsKey(extension)) {
            throw new IllegalArgumentException("Chỉ chấp nhận PDF, tài liệu Word/Excel/PowerPoint và tệp ảnh.");
        }
    }

    private String store(MultipartFile file) {
        String name = UUID.randomUUID() + "." + extension(file.getOriginalFilename());
        try {
            Files.createDirectories(uploadDirectory);
            try (var input = file.getInputStream()) {
                Files.copy(input, uploadDirectory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return name;
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể lưu tệp tài liệu.", e);
        }
    }

    private String normalizeUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        try {
            URI uri = URI.create(trimmed);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
                throw new IllegalArgumentException("Liên kết tài liệu phải bắt đầu bằng http:// hoặc https://.");
            }
            return trimmed;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Liên kết tài liệu")) throw e;
            throw new IllegalArgumentException("Liên kết tài liệu không hợp lệ.");
        }
    }

    private String cleanOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) return "tai-lieu";
        String clean = filename.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1).trim();
        return clean.length() > 255 ? clean.substring(clean.length() - 255) : clean;
    }

    private String extension(String filename) {
        String clean = cleanOriginalFilename(filename);
        int dot = clean.lastIndexOf('.');
        return dot < 0 ? "" : clean.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String contentTypeFor(String filename) {
        return ALLOWED_TYPES.get(extension(filename));
    }

    private LearningMaterial getMaterial(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu."));
    }

    private void requireFile(LearningMaterial material) {
        Path path = resolveStoredFile(material);
        if (!Files.isRegularFile(path)) throw new IllegalArgumentException("Tệp tài liệu không còn tồn tại trên máy chủ.");
    }

    private void requireTeacherOwns(ClassEntity classEntity, Long teacherUserId) {
        if (classEntity.getTeacher() == null || classEntity.getTeacher().getUser() == null
                || !Objects.equals(classEntity.getTeacher().getUser().getId(), teacherUserId)) {
            throw new IllegalArgumentException("Bạn chỉ được quản lý tài liệu của lớp mình phụ trách.");
        }
    }

    private void delete(LearningMaterial material) {
        materialRepository.delete(material);
        if (material.getStoredFilename() != null) deleteStoredFile(material.getStoredFilename());
    }

    private void deleteStoredFile(String filename) {
        try {
            Path path = uploadDirectory.resolve(filename).normalize();
            if (path.startsWith(uploadDirectory)) Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The database entry is authoritative; an orphaned file can be cleaned up later.
        }
    }
}
