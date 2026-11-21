package com.educenter.service.impl;

import com.educenter.config.AIProperties;
import com.educenter.dto.AISettingsUpdate;
import com.educenter.dto.AIUsageSummary;
import com.educenter.entity.AIConversation;
import com.educenter.entity.AISettings;
import com.educenter.entity.AIUsage;
import com.educenter.repository.AISettingsRepository;
import com.educenter.repository.AIUsageRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.AIException;
import com.educenter.service.AIUsageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AIUsageServiceImpl implements AIUsageService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<String> ROLES = List.of("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_PARENT", "ROLE_STUDENT");
    private final AISettingsRepository settingsRepository;
    private final AIUsageRepository usageRepository;
    private final UserRepository userRepository;
    private final AIProperties properties;

    public AIUsageServiceImpl(AISettingsRepository settingsRepository, AIUsageRepository usageRepository,
                              UserRepository userRepository, AIProperties properties) {
        this.settingsRepository = settingsRepository;
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public AIUsage reserve(Long userId, String role, AIConversation conversation, String model, int promptChars) {
        AISettings settings = settingsRepository.findSingletonForUpdate().orElseGet(this::createDisabledSettings);
        validateEnabled(settings, role);
        validateModel(settings, model);
        if (promptChars < 1 || promptChars > settings.getMaxPromptCharacters())
            throw new AIException("PROMPT_LIMIT", null, false, "Câu hỏi vượt quá độ dài được phép.");
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDateTime from = today.atStartOfDay(), to = today.atTime(LocalTime.MAX);
        if (usageRepository.countByUserIdAndCreatedAtBetween(userId, from, to) >= settings.getPerUserDailyRequests())
            throw new AIException("USER_DAILY_LIMIT", null, false, "Bạn đã đạt giới hạn sử dụng AI trong ngày.");
        if (usageRepository.countByCreatedAtBetween(from, to) >= settings.getGlobalDailyRequests())
            throw new AIException("GLOBAL_DAILY_LIMIT", null, false, "Hùng Cường AI đã đạt giới hạn sử dụng miễn phí. Vui lòng thử lại sau.");
        if (usageRepository.countPending() >= settings.getMaxConcurrentRequests())
            throw new AIException("CONCURRENT_LIMIT", null, false, "Hùng Cường AI đang xử lý đủ số yêu cầu đồng thời. Vui lòng thử lại sau.");
        long reserved = Math.max(1, (promptChars + 3L) / 4L) + settings.getMaxResponseTokens();
        if (usageRepository.sumReservedTokensBetween(from, to) + reserved > settings.getDailyTokenLimit())
            throw new AIException("TOKEN_DAILY_LIMIT", null, false, "Hùng Cường AI đã đạt giới hạn sử dụng miễn phí. Vui lòng thử lại sau.");

        AIUsage usage = new AIUsage();
        usage.setUser(userRepository.getReferenceById(userId));
        usage.setConversation(conversation);
        usage.setRoleName(role);
        usage.setModel(model);
        usage.setStatus("PENDING");
        usage.setReservedTokens(reserved);
        return usageRepository.saveAndFlush(usage);
    }

    @Override
    @Transactional
    public void complete(Long usageId, long promptTokens, long completionTokens, Integer providerStatus,
                         Long remainingRequests, Long remainingTokens) {
        AIUsage usage = usageRepository.findById(usageId).orElseThrow();
        usage.setStatus("COMPLETED");
        usage.setPromptTokens(Math.max(0, promptTokens));
        usage.setCompletionTokens(Math.max(0, completionTokens));
        long actual = promptTokens + completionTokens;
        if (actual > 0) usage.setReservedTokens(actual);
        usage.setProviderStatus(providerStatus);
        usage.setProviderRemainingRequests(remainingRequests);
        usage.setProviderRemainingTokens(remainingTokens);
        usage.setCompletedAt(LocalDateTime.now(APP_ZONE));
        usageRepository.save(usage);
    }

    @Override
    @Transactional
    public void fail(Long usageId, AIException error) {
        usageRepository.findById(usageId).ifPresent(usage -> {
            usage.setStatus("FAILED");
            usage.setErrorCode(error.getCode());
            usage.setProviderStatus(error.getProviderStatus());
            usage.setCompletedAt(LocalDateTime.now(APP_ZONE));
            usageRepository.save(usage);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AIUsageSummary summary(Long userId) {
        AISettings settings = settingsRepository.findById(1).orElse(null);
        LocalDateTime from = LocalDate.now(APP_ZONE).atStartOfDay(), to = LocalDate.now(APP_ZONE).atTime(LocalTime.MAX);
        long requests = usageRepository.countByUserIdAndCreatedAtBetween(userId, from, to);
        long tokens = usageRepository.sumUserTokensBetween(userId, from, to);
        long failed = usageRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, "FAILED", from, to);
        long remainingRequests = settings == null || settings.getPerUserDailyRequests() == null ? 0
                : Math.max(0, settings.getPerUserDailyRequests() - requests);
        long remainingTokens = settings == null || settings.getDailyTokenLimit() == null ? 0
                : Math.max(0, settings.getDailyTokenLimit() - usageRepository.sumReservedTokensBetween(from, to));
        return new AIUsageSummary(requests, tokens, failed, remainingRequests, remainingTokens, Map.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> adminSummary() {
        LocalDateTime from = LocalDate.now(APP_ZONE).atStartOfDay(), to = LocalDate.now(APP_ZONE).atTime(LocalTime.MAX);
        long requests = usageRepository.countByCreatedAtBetween(from, to);
        long tokens = usageRepository.sumTokensBetween(from, to);
        long failed = usageRepository.countByStatusAndCreatedAtBetween("FAILED", from, to);
        Map<String, Long> byRole = ROLES.stream().collect(Collectors.toMap(role -> role,
                role -> usageRepository.countByRoleNameAndCreatedAtBetween(role, from, to), (a, b) -> a, LinkedHashMap::new));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestsToday", requests);
        result.put("tokensToday", tokens);
        result.put("failedToday", failed);
        result.put("pending", usageRepository.countPending());
        result.put("requestsByRole", byRole);
        result.put("remainingProviderRequests", latestProviderMetric(true));
        result.put("remainingProviderTokens", latestProviderMetric(false));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AISettings settings() { return settingsRepository.findById(1).orElseGet(this::newDisabledSettings); }

    @Override
    @Transactional
    public AISettings updateSettings(AISettingsUpdate update) {
        requirePositive(update.perUserDailyRequests(), "Giới hạn theo tài khoản");
        requirePositive(update.globalDailyRequests(), "Giới hạn toàn hệ thống");
        requirePositive(update.dailyTokenLimit(), "Giới hạn token");
        requirePositive(update.maxPromptCharacters(), "Độ dài câu hỏi");
        requirePositive(update.maxResponseTokens(), "Độ dài câu trả lời");
        requirePositive(update.maxConcurrentRequests(), "Số yêu cầu đồng thời");
        if (update.maxPromptCharacters() > 50000 || update.maxResponseTokens() > 8192 || update.maxConcurrentRequests() > 50
                || update.perUserDailyRequests() > 10000 || update.globalDailyRequests() > 100000
                || update.dailyTokenLimit() > 1_000_000_000L)
            throw new IllegalArgumentException("Một hoặc nhiều giới hạn vượt mức tối đa cho phép.");
        AISettings settings = settingsRepository.findSingletonForUpdate().orElseGet(this::createDisabledSettings);
        Set<String> allowed = allowedModels();
        if (update.selectedModel() == null || !allowed.contains(update.selectedModel()))
            throw new IllegalArgumentException("Mô hình không nằm trong danh sách Groq được cho phép.");
        settings.setPerUserDailyRequests(update.perUserDailyRequests());
        settings.setGlobalDailyRequests(update.globalDailyRequests());
        settings.setDailyTokenLimit(update.dailyTokenLimit());
        settings.setMaxPromptCharacters(update.maxPromptCharacters());
        settings.setMaxResponseTokens(update.maxResponseTokens());
        settings.setMaxConcurrentRequests(update.maxConcurrentRequests());
        settings.setAllowedModels(String.join(",", allowed));
        settings.setSelectedModel(update.selectedModel());
        settings.setAdminEnabled(update.adminEnabled());
        settings.setTeacherEnabled(update.teacherEnabled());
        settings.setParentEnabled(update.parentEnabled());
        settings.setStudentEnabled(update.studentEnabled());
        settings.setEnabled(update.enabled());
        return settingsRepository.save(settings);
    }

    private void validateEnabled(AISettings s, String role) {
        if (!properties.isEnabled() || !properties.getGroq().hasApiKey())
            throw new AIException("AI_UNAVAILABLE", null, false, "Hùng Cường AI hiện chưa được cấu hình.");
        if (!s.isEnabled() || !limitsConfigured(s))
            throw new AIException("AI_DISABLED", null, false, "Hùng Cường AI chưa được bật hoặc chưa được Admin cấu hình hạn mức.");
        boolean enabled = switch (role) {
            case "ROLE_ADMIN" -> s.isAdminEnabled();
            case "ROLE_TEACHER" -> s.isTeacherEnabled();
            case "ROLE_PARENT" -> s.isParentEnabled();
            case "ROLE_STUDENT" -> s.isStudentEnabled();
            default -> false;
        };
        if (!enabled) throw new AIException("ROLE_DISABLED", null, false, "Hùng Cường AI chưa được bật cho vai trò của bạn.");
    }

    private boolean limitsConfigured(AISettings s) {
        return positive(s.getPerUserDailyRequests()) && positive(s.getGlobalDailyRequests())
                && positive(s.getDailyTokenLimit()) && positive(s.getMaxPromptCharacters())
                && positive(s.getMaxResponseTokens()) && positive(s.getMaxConcurrentRequests());
    }
    private boolean positive(Number n) { return n != null && n.longValue() > 0; }
    private void requirePositive(Number n, String label) {
        if (!positive(n)) throw new IllegalArgumentException(label + " phải lớn hơn 0.");
    }
    private void validateModel(AISettings s, String model) {
        if (!allowedModels().contains(model) || !model.equals(s.getSelectedModel()))
            throw new AIException("MODEL_NOT_ALLOWED", null, false, "Mô hình AI đã cấu hình không hợp lệ.");
    }
    private Set<String> allowedModels() {
        return java.util.Arrays.stream(properties.getGroq().getAllowedModels().split(","))
                .map(String::trim).filter(value -> value.matches("[A-Za-z0-9._-]{1,60}/[A-Za-z0-9._-]{1,80}"))
                .collect(Collectors.toUnmodifiableSet());
    }
    private AISettings createDisabledSettings() {
        return settingsRepository.save(newDisabledSettings());
    }
    private AISettings newDisabledSettings() {
        AISettings settings = new AISettings();
        Set<String> allowed = allowedModels();
        settings.setAllowedModels(String.join(",", allowed));
        settings.setSelectedModel(allowed.contains(properties.getGroq().getModel()) ? properties.getGroq().getModel()
                : allowed.stream().findFirst().orElse("openai/gpt-oss-120b"));
        return settings;
    }
    private Long latestProviderMetric(boolean requests) {
        return (requests ? usageRepository.findTopByProviderRemainingRequestsIsNotNullOrderByCreatedAtDescIdDesc()
                : usageRepository.findTopByProviderRemainingTokensIsNotNullOrderByCreatedAtDescIdDesc())
                .filter(usage -> usage.getCreatedAt() != null
                        && usage.getCreatedAt().toLocalDate().equals(LocalDate.now(APP_ZONE)))
                .map(usage -> requests ? usage.getProviderRemainingRequests() : usage.getProviderRemainingTokens())
                .orElse(null);
    }
}
