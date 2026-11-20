package com.educenter.repository;

import com.educenter.entity.AIUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AIUsageRepository extends JpaRepository<AIUsage, Long> {
    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
    long countByUserIdAndStatusAndCreatedAtBetween(Long userId, String status, LocalDateTime from, LocalDateTime to);
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByStatus(String status);
    long countByRoleNameAndCreatedAtBetween(String roleName, LocalDateTime from, LocalDateTime to);
    long countByStatusAndCreatedAtBetween(String status, LocalDateTime from, LocalDateTime to);
    Optional<AIUsage> findTopByProviderRemainingRequestsIsNotNullOrderByCreatedAtDescIdDesc();
    Optional<AIUsage> findTopByProviderRemainingTokensIsNotNullOrderByCreatedAtDescIdDesc();
    @Query("select coalesce(sum(u.promptTokens + u.completionTokens), 0) from AIUsage u where u.createdAt between :from and :to")
    long sumTokensBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select coalesce(sum(u.promptTokens + u.completionTokens), 0) from AIUsage u where u.user.id = :userId and u.createdAt between :from and :to")
    long sumUserTokensBetween(@Param("userId") Long userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select coalesce(sum(u.reservedTokens), 0) from AIUsage u where u.createdAt between :from and :to")
    long sumReservedTokensBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select count(u) from AIUsage u where u.status = 'PENDING'")
    long countPending();
}
