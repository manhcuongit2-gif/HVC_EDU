package com.educenter.service;

public class AIException extends RuntimeException {
    private final String code;
    private final Integer providerStatus;
    private final boolean retryable;
    private final String userMessage;

    public AIException(String code, Integer providerStatus, boolean retryable, String userMessage) {
        super(userMessage);
        this.code = code;
        this.providerStatus = providerStatus;
        this.retryable = retryable;
        this.userMessage = userMessage;
    }
    public String getCode() { return code; }
    public Integer getProviderStatus() { return providerStatus; }
    public boolean isRetryable() { return retryable; }
    public String getUserMessage() { return userMessage; }
}
