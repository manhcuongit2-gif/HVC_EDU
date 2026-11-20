package com.educenter.repository;

import com.educenter.entity.AIConversation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {
    @Query(value = "select c.* from ai_conversations c where c.user_id = :userId " +
            "and (:query is null or lower(c.title) like concat('%', lower(:query), '%') " +
            "or exists (select 1 from ai_messages m where m.conversation_id = c.id " +
            "and lower(cast(m.content as char)) like concat('%', lower(:query), '%')) ) " +
            "order by c.pinned desc, c.updated_at desc", nativeQuery = true)
    List<AIConversation> searchOwned(@Param("userId") Long userId, @Param("query") String query);
    Optional<AIConversation> findByIdAndOwnerId(Long id, Long ownerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AIConversation c where c.id = :id and c.owner.id = :ownerId")
    Optional<AIConversation> findByIdAndOwnerIdForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
