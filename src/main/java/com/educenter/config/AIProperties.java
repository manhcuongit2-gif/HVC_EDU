package com.educenter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "ai")
public class AIProperties {
    private boolean enabled = true;
    private String provider = "groq";
    private Groq groq = new Groq();
    private Context context = new Context();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Groq getGroq() { return groq; }
    public void setGroq(Groq groq) { this.groq = groq; }
    public Context getContext() { return context; }
    public void setContext(Context context) { this.context = context; }

    public static class Groq {
        private String apiKey = "";
        private String baseUrl = "https://api.groq.com/openai/v1";
        private String model = "openai/gpt-oss-120b";
        private String allowedModels = "openai/gpt-oss-120b,openai/gpt-oss-20b,qwen/qwen3.8-27b";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration requestTimeout = Duration.ofSeconds(90);
        private long maxUploadBytes = 10_485_760;
        private int maxExtractedCharacters = 40_000;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey.trim(); }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getAllowedModels() { return allowedModels; }
        public void setAllowedModels(String allowedModels) { this.allowedModels = allowedModels; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
        public Duration getRequestTimeout() { return requestTimeout; }
        public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
        public long getMaxUploadBytes() { return maxUploadBytes; }
        public void setMaxUploadBytes(long maxUploadBytes) { this.maxUploadBytes = maxUploadBytes; }
        public int getMaxExtractedCharacters() { return maxExtractedCharacters; }
        public void setMaxExtractedCharacters(int maxExtractedCharacters) { this.maxExtractedCharacters = maxExtractedCharacters; }
        public boolean hasApiKey() { return apiKey != null && !apiKey.isBlank(); }
        public boolean isVisionModel(String model) { return "qwen/qwen3.8-27b".equals(model); }
    }

    public static class Context {
        private int maxMessages = 12;
        private int maxCharacters = 24_000;
        public int getMaxMessages() { return maxMessages; }
        public void setMaxMessages(int maxMessages) { this.maxMessages = maxMessages; }
        public int getMaxCharacters() { return maxCharacters; }
        public void setMaxCharacters(int maxCharacters) { this.maxCharacters = maxCharacters; }
    }
}
