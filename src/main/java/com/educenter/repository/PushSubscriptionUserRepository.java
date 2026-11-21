package com.educenter.repository;

import com.educenter.entity.PushSubscriptionUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionUserRepository extends JpaRepository<PushSubscriptionUser, Long> {
    @EntityGraph(attributePaths = "subscription")
    List<PushSubscriptionUser> findByUserIdAndActiveTrueAndSubscriptionActiveTrueOrderBySubscriptionLastSeenAtDesc(Long userId);
    Optional<PushSubscriptionUser> findBySubscriptionIdAndUserId(Long subscriptionId, Long userId);
    Optional<PushSubscriptionUser> findByIdAndUserIdAndActiveTrue(Long id, Long userId);
}
