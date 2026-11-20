package com.educenter.repository;

import com.educenter.entity.NotificationBroadcast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationBroadcastRepository extends JpaRepository<NotificationBroadcast, Long> {
    List<NotificationBroadcast> findTop50ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(String status, LocalDateTime now);
    List<NotificationBroadcast> findTop100ByOrderByCreatedAtDesc();
}
