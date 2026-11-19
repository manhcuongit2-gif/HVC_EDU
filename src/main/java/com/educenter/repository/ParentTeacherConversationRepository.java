package com.educenter.repository;

import com.educenter.entity.ParentTeacherConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParentTeacherConversationRepository extends JpaRepository<ParentTeacherConversation, Long> {
    Optional<ParentTeacherConversation> findByClassEntityIdAndParentId(Long classId, Long parentId);

    @Query("select c from ParentTeacherConversation c join fetch c.classEntity ce join fetch c.parent p join fetch p.user " +
            "where ce.teacher.user.id = :teacherUserId order by c.updatedAt desc")
    List<ParentTeacherConversation> findForTeacher(@Param("teacherUserId") Long teacherUserId);

    List<ParentTeacherConversation> findByParentUserIdOrderByUpdatedAtDesc(Long userId);
}
