package com.educenter.service.impl;

import com.educenter.config.AIProperties;
import com.educenter.entity.ClassStudent;
import com.educenter.entity.LearningMaterial;
import com.educenter.entity.Student;
import com.educenter.entity.StudentAILearningMaterialChunk;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.repository.LearningMaterialRepository;
import com.educenter.repository.StudentAILearningMaterialChunkRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.service.AIDocumentService;
import com.educenter.service.LearningMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StudentAIDataService {
    private static final int CHUNK_SIZE = 1_200;
    private static final int CHUNK_OVERLAP = 180;
    private static final Set<String> INDEXABLE = Set.of("pdf", "doc", "docx", "xls", "xlsx", "txt");
    private static final Set<String> STOP_WORDS = Set.of("cua", "của", "cho", "trong", "một", "những", "được", "là", "và", "các", "theo", "với", "hãy", "giúp", "em", "tôi", "mình", "này", "đó", "thế", "nào", "gì", "về", "từ", "đến", "khi", "nếu", "như", "để", "vì", "sao", "có", "không", "bài", "tập");
    private static final Pattern WORDS = Pattern.compile("[\\p{L}\\p{N}]{3,}");

    private final StudentRepository students;
    private final ClassStudentRepository enrollments;
    private final LearningMaterialRepository materials;
    private final StudentAILearningMaterialChunkRepository chunks;
    private final LearningMaterialService materialService;
    private final AIDocumentService documentService;
    private final AIProperties properties;

    public StudentAIDataService(StudentRepository students, ClassStudentRepository enrollments,
                                LearningMaterialRepository materials,
                                StudentAILearningMaterialChunkRepository chunks,
                                LearningMaterialService materialService, AIDocumentService documentService,
                                AIProperties properties) {
        this.students = students;
        this.enrollments = enrollments;
        this.materials = materials;
        this.chunks = chunks;
        this.materialService = materialService;
        this.documentService = documentService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Student requireLinkedStudent(Long userId) {
        Student student = students.findByUserId(userId)
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa được liên kết với hồ sơ học sinh đang hoạt động."));
        if (student.getUser() == null || !Objects.equals(student.getUser().getId(), userId)
                || student.getUser().getRole() == null
                || !"ROLE_STUDENT".equals(student.getUser().getRole().getName())
                || !Boolean.TRUE.equals(student.getUser().getActive())) {
            throw new IllegalArgumentException("Tài khoản học sinh không hoạt động hoặc chưa được liên kết đúng hồ sơ.");
        }
        return student;
    }

    @Transactional(readOnly = true)
    public String learningProfileContext(Long userId) {
        Student student = requireLinkedStudent(userId);
        Set<Long> allowedClasses = new HashSet<>(activeClassIds(student.getId()));
        List<String> subjects = enrollments.findByStudentId(student.getId()).stream()
                .filter(e -> e.getClassEntity() != null && allowedClasses.contains(e.getClassEntity().getId()))
                .map(e -> e.getClassEntity().getSubject())
                .filter(Objects::nonNull).map(subject -> subject.getName())
                .filter(Objects::nonNull).map(String::trim).filter(name -> !name.isBlank()).distinct().sorted().toList();
        StringBuilder summary = new StringBuilder("Thông tin học tập để điều chỉnh cách giải thích: ");
        if (student.getGradeLevel() != null && !student.getGradeLevel().isBlank())
            summary.append("khối/lớp ghi trong hồ sơ là ").append(student.getGradeLevel().trim()).append("; ");
        if (!subjects.isEmpty()) summary.append("môn đang học gồm ").append(String.join(", ", subjects)).append("; ");
        if (summary.toString().endsWith(": ")) summary.append("chưa có khối lớp hoặc môn học được ghi nhận.");
        return summary.toString();
    }

    @Transactional(readOnly = true)
    public String sourceVersion(Long userId) {
        Student student = requireLinkedStudent(userId);
        List<Long> classIds = activeClassIds(student.getId());
        if (classIds.isEmpty()) return "no-active-materials";
        return materials.findByClassEntityIdInOrderByCreatedAtDesc(classIds).stream()
                .sorted(Comparator.comparing(LearningMaterial::getId))
                .map(material -> material.getId() + ":" + material.getClassEntity().getId() + ":"
                        + Objects.toString(material.getTitle(), "") + ":" + Objects.toString(material.getDescription(), "") + ":"
                        + Objects.toString(material.getStoredFilename(), "") + ":" + Objects.toString(material.getExternalUrl(), ""))
                .collect(Collectors.joining("|"));
    }

    @Transactional
    public List<RetrievedSource> retrieveForStudent(Long userId, String query, int limit) {
        Student student = requireLinkedStudent(userId);
        List<Long> classIds = activeClassIds(student.getId());
        if (classIds.isEmpty()) return List.of();

        for (LearningMaterial material : materials.findByClassEntityIdInOrderByCreatedAtDesc(classIds)) {
            if ("PENDING".equalsIgnoreCase(material.getAiIndexStatus())) indexMaterial(material);
        }
        List<StudentAILearningMaterialChunk> allChunks = chunks.findForClasses(classIds);
        Set<String> terms = terms(query);
        return allChunks.stream().map(chunk -> new ScoredChunk(chunk, score(chunk.getContent(), terms)))
                .filter(item -> terms.isEmpty() || item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed()
                        .thenComparing(item -> item.chunk().getMaterial().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparingInt(item -> item.chunk().getChunkIndex()))
                .limit(Math.max(1, Math.min(limit, 8)))
                .map(item -> source(item.chunk()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> activeClassIds(Long studentId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        return enrollments.findByStudentId(studentId).stream()
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .filter(e -> e.getEnrolledDate() == null || !e.getEnrolledDate().isAfter(today))
                .filter(e -> e.getDroppedDate() == null || e.getDroppedDate().isAfter(today))
                .map(ClassStudent::getClassEntity)
                .filter(Objects::nonNull)
                .filter(c -> c.getId() != null && (c.getStatus() == null || "ACTIVE".equals(c.getStatus())))
                .map(c -> c.getId()).distinct().toList();
    }

    private void indexMaterial(LearningMaterial material) {
        String filename = material.getOriginalFilename();
        String extension = extension(filename);
        if (!INDEXABLE.contains(extension) || material.getStoredFilename() == null || material.getStoredFilename().isBlank()) {
            chunks.deleteByMaterialId(material.getId());
            material.setAiIndexStatus("UNSUPPORTED");
            materials.save(material);
            return;
        }
        Path path;
        List<AIDocumentService.DocumentPart> parts;
        try {
            path = materialService.resolveStoredFile(material);
            if (!Files.isRegularFile(path) || Files.size(path) > properties.getGroq().getMaxUploadBytes()) {
                chunks.deleteByMaterialId(material.getId());
                material.setAiIndexStatus("UNSUPPORTED");
                materials.save(material);
                return;
            }
            parts = documentService.extractParts(Files.readAllBytes(path), filename);
        } catch (IOException | IllegalArgumentException ex) {
            chunks.deleteByMaterialId(material.getId());
            material.setAiIndexStatus("FAILED");
            materials.save(material);
            return;
        }
        List<StudentAILearningMaterialChunk> extracted = new ArrayList<>();
        int index = 0;
        for (AIDocumentService.DocumentPart part : parts) {
            for (String text : split(part.text())) {
                StudentAILearningMaterialChunk chunk = new StudentAILearningMaterialChunk();
                chunk.setMaterial(material);
                chunk.setChunkIndex(index++);
                chunk.setPageNumber(part.pageNumber());
                chunk.setSectionName(part.sectionName());
                chunk.setContent(text);
                extracted.add(chunk);
            }
        }
        chunks.deleteByMaterialId(material.getId());
        if (extracted.isEmpty()) material.setAiIndexStatus("UNSUPPORTED");
        else {
            chunks.saveAll(extracted);
            material.setAiIndexStatus("READY");
        }
        materials.save(material);
    }

    private List<String> split(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalized = text.replace('\u0000', ' ').replaceAll("\\s+", " ").trim();
        List<String> result = new ArrayList<>();
        for (int start = 0; start < normalized.length();) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            if (end < normalized.length()) {
                int space = normalized.lastIndexOf(' ', end);
                if (space > start + CHUNK_SIZE / 2) end = space;
            }
            String part = normalized.substring(start, end).trim();
            if (!part.isBlank()) result.add(part);
            if (end >= normalized.length()) break;
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        return result;
    }

    private RetrievedSource source(StudentAILearningMaterialChunk chunk) {
        String location = chunk.getPageNumber() != null ? "trang " + chunk.getPageNumber()
                : chunk.getSectionName() != null && !chunk.getSectionName().isBlank() ? chunk.getSectionName() : "tài liệu lớp";
        return new RetrievedSource(chunk.getMaterial().getId(), chunk.getMaterial().getTitle(), location, chunk.getContent());
    }

    private Set<String> terms(String query) {
        if (query == null || query.isBlank()) return Set.of();
        var matcher = WORDS.matcher(query.toLowerCase(Locale.ROOT));
        Set<String> result = new LinkedHashSet<>();
        while (matcher.find()) {
            String term = matcher.group();
            if (!STOP_WORDS.contains(term)) result.add(term);
        }
        return result;
    }

    private int score(String content, Set<String> terms) {
        if (terms.isEmpty()) return 0;
        String lower = content == null ? "" : content.toLowerCase(Locale.ROOT);
        return (int) terms.stream().filter(lower::contains).count();
    }

    private String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record RetrievedSource(Long materialId, String title, String location, String excerpt) { }
    private record ScoredChunk(StudentAILearningMaterialChunk chunk, int score) { }
}
