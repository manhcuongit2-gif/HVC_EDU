package com.educenter.repository;

import com.educenter.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    List<PushSubscription> findByUserIdAndActiveTrueOrderByLastSeenAtDesc(Long userId);
    Optional<PushSubscription> findByEndpointHash(String endpointHash);
    Optional<PushSubscription> findByIdAndUserId(Long id, Long userId);
}
