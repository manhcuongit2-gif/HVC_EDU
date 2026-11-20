package com.educenter.repository;

import com.educenter.entity.AISettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface AISettingsRepository extends JpaRepository<AISettings, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AISettings s where s.id = 1")
    Optional<AISettings> findSingletonForUpdate();
}
