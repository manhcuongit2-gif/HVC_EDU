package com.educenter.repository;

import com.educenter.entity.UserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findByUserIdAndHiddenAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserIdAndReadAtIsNullAndHiddenAtIsNull(Long userId);
    Optional<UserNotification> findByIdAndUserId(Long id, Long userId);
    Optional<UserNotification> findByUserIdAndEventKey(Long userId, String eventKey);
    boolean existsByUserIdAndEventKey(Long userId, String eventKey);
    @Modifying
    @Query("update UserNotification n set n.readAt = :now where n.user.id = :userId and n.readAt is null and n.hiddenAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") java.time.LocalDateTime now);
}
