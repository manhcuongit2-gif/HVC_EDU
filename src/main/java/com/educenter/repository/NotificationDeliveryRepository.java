package com.educenter.repository;

import com.educenter.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
    boolean existsByOutboxIdAndSubscriptionId(Long outboxId, Long subscriptionId);
    Optional<NotificationDelivery> findByOutboxIdAndSubscriptionId(Long outboxId, Long subscriptionId);
}
