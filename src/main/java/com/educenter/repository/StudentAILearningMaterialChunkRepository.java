package com.educenter.repository;

import com.educenter.entity.StudentAILearningMaterialChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StudentAILearningMaterialChunkRepository extends JpaRepository<StudentAILearningMaterialChunk, Long> {
    void deleteByMaterialId(Long materialId);

    @Query("select c from StudentAILearningMaterialChunk c join fetch c.material m " +
            "where m.classEntity.id in :classIds order by m.createdAt desc, c.chunkIndex asc")
    List<StudentAILearningMaterialChunk> findForClasses(@Param("classIds") List<Long> classIds);
}
