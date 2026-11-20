package com.educenter.repository;

import com.educenter.entity.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    List<NotificationOutbox> findTop100ByStatusAndScheduledAtLessThanEqualAndNextAttemptAtIsNullOrderByScheduledAtAsc(String status, LocalDateTime now);
    List<NotificationOutbox> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(String status, LocalDateTime now);
    boolean existsByEventKey(String eventKey);
}
