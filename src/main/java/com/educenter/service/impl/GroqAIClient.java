package com.educenter.service.impl;

import com.educenter.config.AIProperties;
import com.educenter.service.AIException;
import com.educenter.service.AIProvider;
import com.educenter.service.AIProvider.AIProviderResponse;
import com.educenter.service.AIProvider.ToolCall;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Component
public class GroqAIClient implements AIProvider {
    private static final ScheduledExecutorService STREAM_TIMEOUTS = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "groq-stream-timeouts");
        thread.setDaemon(true);
        return thread;
    });
    private final AIProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GroqAIClient(AIProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getGroq().getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public AIProviderResponse complete(List<Map<String, Object>> messages, String model, int maxTokens,
                                       List<Map<String, Object>> tools) {
        Map<String, Object> body = baseBody(messages, model, maxTokens);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }
        HttpResponse<String> response = send(body, "application/json");
        validateStatus(response.statusCode());
        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choice = root.path("choices").path(0);
            String content = choice.path("message").path("content").asText("");
            List<ToolCall> toolCalls = new ArrayList<>();
            JsonNode calls = choice.path("message").path("tool_calls");
            if (calls.isArray()) {
                for (JsonNode call : calls) {
                    toolCalls.add(new ToolCall(call.path("id").asText(""),
                            call.path("function").path("name").asText(""),
                            call.path("function").path("arguments").asText("{}")));
                }
            }
            JsonNode usage = root.path("usage");
            return new AIProviderResponse(content, toolCalls,
                    usage.path("prompt_tokens").asLong(0), usage.path("completion_tokens").asLong(0),
                    response.statusCode(), headerLong(response, "x-ratelimit-remaining-requests"),
                    headerLong(response, "x-ratelimit-remaining-tokens"));
        } catch (IOException e) {
            throw new AIException("INVALID_PROVIDER_RESPONSE", response.statusCode(), false,
                    "Dịch vụ AI trả về dữ liệu không hợp lệ.");
        }
    }

    @Override
    public AIProviderResponse stream(List<Map<String, Object>> messages, String model, int maxTokens,
                                    Consumer<String> onDelta) {
        Map<String, Object> body = baseBody(messages, model, maxTokens);
        body.put("stream", true);
        body.put("stream_options", Map.of("include_usage", true));
        HttpRequest request = request(body, "text/event-stream");
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (InputStream ignored = response.body()) { }
                throw statusException(response.statusCode());
            }
            long promptTokens = 0;
            long completionTokens = 0;
            AtomicBoolean streamClosed = new AtomicBoolean();
            AtomicBoolean timedOut = new AtomicBoolean();
            ScheduledFuture<?> watchdog = STREAM_TIMEOUTS.schedule(() -> {
                if (streamClosed.compareAndSet(false, true)) {
                    timedOut.set(true);
                    try { response.body().close(); } catch (IOException ignored) { }
                }
            }, Math.max(1, properties.getGroq().getRequestTimeout().toMillis()), TimeUnit.MILLISECONDS);
            try (InputStream input = response.body();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    JsonNode root;
                    try { root = objectMapper.readTree(data); }
                    catch (IOException ignored) { continue; }
                    JsonNode usage = root.path("usage");
                    if (!usage.isMissingNode() && !usage.isNull()) {
                        promptTokens = usage.path("prompt_tokens").asLong(promptTokens);
                        completionTokens = usage.path("completion_tokens").asLong(completionTokens);
                    }
                    JsonNode delta = root.path("choices").path(0).path("delta").path("content");
                    if (delta.isTextual() && !delta.asText().isEmpty()) onDelta.accept(delta.asText());
                }
                streamClosed.set(true);
            } catch (IOException e) {
                if (timedOut.get()) throw new AIException("TIMEOUT", null, true, "Dịch vụ AI phản hồi quá thời gian.");
                throw e;
            } finally {
                streamClosed.set(true);
                watchdog.cancel(false);
            }
            if (timedOut.get()) throw new AIException("TIMEOUT", null, true, "Dịch vụ AI phản hồi quá thời gian.");
            return new AIProviderResponse("", List.of(), promptTokens, completionTokens, response.statusCode(),
                    headerLong(response, "x-ratelimit-remaining-requests"),
                    headerLong(response, "x-ratelimit-remaining-tokens"));
        } catch (AIException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIException("INTERRUPTED", null, false, "Yêu cầu AI đã bị dừng.");
        } catch (IOException e) {
            Throwable cause = e.getCause();
            boolean timeout = e instanceof java.net.http.HttpTimeoutException || cause instanceof TimeoutException;
            boolean retryable = timeout || e instanceof ConnectException || cause instanceof ConnectException;
            throw new AIException(timeout ? "TIMEOUT" : "CONNECTION", null, retryable,
                    timeout ? "Dịch vụ AI phản hồi quá thời gian." : "Không thể kết nối dịch vụ AI.");
        }
    }

    private Map<String, Object> baseBody(List<Map<String, Object>> messages, String model, int maxTokens) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_completion_tokens", maxTokens);
        body.put("temperature", 0.4);
        return body;
    }

    private HttpResponse<String> send(Map<String, Object> body, String accept) {
        try {
            return httpClient.send(request(body, accept), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIException("INTERRUPTED", null, false, "Yêu cầu AI đã bị dừng.");
        } catch (IOException | CompletionException e) {
            Throwable cause = e.getCause();
            boolean timeout = e instanceof java.net.http.HttpTimeoutException || cause instanceof TimeoutException;
            boolean retryable = timeout || e instanceof ConnectException || cause instanceof ConnectException;
            throw new AIException(timeout ? "TIMEOUT" : "CONNECTION", null, retryable,
                    timeout ? "Dịch vụ AI phản hồi quá thời gian." : "Không thể kết nối dịch vụ AI.");
        }
    }

    private HttpRequest request(Map<String, Object> body, String accept) {
        if (!properties.getGroq().hasApiKey()) {
            throw new AIException("MISSING_API_KEY", null, false, "Hùng Cường AI chưa được cấu hình khóa API.");
        }
        try {
            String baseUrl = properties.getGroq().getBaseUrl().replaceAll("/+$", "");
            return HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(properties.getGroq().getRequestTimeout())
                    .header("Authorization", "Bearer " + properties.getGroq().getApiKey())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", accept)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
        } catch (Exception e) {
            throw new AIException("REQUEST_BUILD", null, false, "Không thể tạo yêu cầu AI.");
        }
    }

    private void validateStatus(int status) {
        if (status < 200 || status >= 300) throw statusException(status);
    }

    private AIException statusException(int status) {
        if (status == 401 || status == 403)
            return new AIException("INVALID_API_KEY", status, false, "Khóa Groq API không hợp lệ hoặc không được cấp quyền.");
        if (status == 429)
            return new AIException("RATE_LIMIT", status, false, "Hùng Cường AI đã đạt giới hạn sử dụng miễn phí. Vui lòng thử lại sau.");
        if (status == 408 || status == 500 || status == 502 || status == 503 || status == 504)
            return new AIException("PROVIDER_TEMPORARY", status, true, "Dịch vụ AI tạm thời không khả dụng.");
        return new AIException("PROVIDER_ERROR", status, false, "Dịch vụ AI từ chối yêu cầu.");
    }

    private Long headerLong(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).map(value -> {
            try { return Long.parseLong(value); } catch (NumberFormatException e) { return null; }
        }).orElse(null);
    }
}
