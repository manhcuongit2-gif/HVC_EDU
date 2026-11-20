package com.educenter.dto;

public record PushSubscriptionRequest(String endpoint, Keys keys, String deviceName, String platform) {
    public record Keys(String p256dh, String auth) { }
}
