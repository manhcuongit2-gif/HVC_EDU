package com.educenter.service.impl;

import com.educenter.config.AIProperties;
import com.educenter.entity.AISettings;
import com.educenter.entity.AIUsage;
import com.educenter.entity.User;
import com.educenter.repository.AISettingsRepository;
import com.educenter.repository.AIUsageRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.AIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AIUsageServiceImplTest {
    private AISettingsRepository settingsRepository;
    private AIUsageRepository usageRepository;
    private UserRepository userRepository;
    private AIProperties properties;
    private AIUsageServiceImpl service;
    private AISettings settings;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(AISettingsRepository.class);
        usageRepository = mock(AIUsageRepository.class);
        userRepository = mock(UserRepository.class);
        properties = new AIProperties();
        properties.setEnabled(true);
        properties.getGroq().setApiKey("test-key");
        properties.getGroq().setAllowedModels("openai/gpt-oss-120b");
        properties.getGroq().setModel("openai/gpt-oss-120b");
        settings = enabledSettings();
        when(settingsRepository.findSingletonForUpdate()).thenReturn(Optional.of(settings));
        when(userRepository.getReferenceById(7L)).thenReturn(new User());
        when(usageRepository.saveAndFlush(any(AIUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new AIUsageServiceImpl(settingsRepository, usageRepository, userRepository, properties);
    }

    @Test
    void reserveStoresPendingRequestAndTokenReservation() {
        AIUsage usage = service.reserve(7L, "ROLE_TEACHER", null, "openai/gpt-oss-120b", 100);

        assertEquals("PENDING", usage.getStatus());
        assertNotNull(usage.getUser());
        assertEquals(425L, usage.getReservedTokens()); // 25 prompt tokens plus 400 response tokens
        verify(usageRepository).saveAndFlush(usage);
    }

    @Test
    void reserveRejectsWhenRoleIsDisabled() {
        settings.setTeacherEnabled(false);

        AIException error = assertThrows(AIException.class,
                () -> service.reserve(7L, "ROLE_TEACHER", null, "openai/gpt-oss-120b", 100));

        assertEquals("ROLE_DISABLED", error.getCode());
        verify(usageRepository, never()).saveAndFlush(any());
    }

    @Test
    void reserveRejectsWhenPerUserDailyQuotaIsSpent() {
        when(usageRepository.countByUserIdAndCreatedAtBetween(eq(7L), any(), any())).thenReturn(3L);

        AIException error = assertThrows(AIException.class,
                () -> service.reserve(7L, "ROLE_TEACHER", null, "openai/gpt-oss-120b", 100));

        assertEquals("USER_DAILY_LIMIT", error.getCode());
        verify(usageRepository, never()).saveAndFlush(any());
    }

    @Test
    void settingsReadDoesNotWriteMissingSingletonRow() {
        when(settingsRepository.findById(1)).thenReturn(Optional.empty());

        AISettings defaults = service.settings();

        assertFalse(defaults.isEnabled());
        assertFalse(defaults.isTeacherEnabled());
        verify(settingsRepository, never()).save(any());
    }

    private AISettings enabledSettings() {
        AISettings result = new AISettings();
        result.setEnabled(true);
        result.setAdminEnabled(true);
        result.setTeacherEnabled(true);
        result.setParentEnabled(true);
        result.setPerUserDailyRequests(3);
        result.setGlobalDailyRequests(30);
        result.setDailyTokenLimit(10_000L);
        result.setMaxPromptCharacters(50_000);
        result.setMaxResponseTokens(400);
        result.setMaxConcurrentRequests(10);
        result.setAllowedModels("openai/gpt-oss-120b");
        result.setSelectedModel("openai/gpt-oss-120b");
        return result;
    }
}
