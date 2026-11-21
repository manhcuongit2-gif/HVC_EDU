package com.educenter.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_ai_learning_material_chunks", uniqueConstraints =
        @UniqueConstraint(name = "uk_student_ai_material_chunk", columnNames = {"material_id", "chunk_index"}))
public class StudentAILearningMaterialChunk {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private LearningMaterial material;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "section_name", length = 255)
    private String sectionName;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    public Long getId() { return id; }
    public LearningMaterial getMaterial() { return material; }
    public void setMaterial(LearningMaterial material) { this.material = material; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
