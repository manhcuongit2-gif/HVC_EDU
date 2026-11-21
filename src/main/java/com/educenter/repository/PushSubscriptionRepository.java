package com.educenter.repository;

import com.educenter.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    @Query("select l.subscription from PushSubscriptionUser l where l.user.id = :userId " +
            "and l.active = true and l.subscription.active = true order by l.subscription.lastSeenAt desc")
    List<PushSubscription> findActiveForLinkedUser(@Param("userId") Long userId);
    Optional<PushSubscription> findByEndpointHash(String endpointHash);
}
