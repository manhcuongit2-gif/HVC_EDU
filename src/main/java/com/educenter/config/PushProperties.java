package com.educenter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.push.vapid")
public record PushProperties(String publicKey, String privateKey, String subject, boolean enabled) {
    public boolean configured() {
        return enabled && publicKey != null && !publicKey.isBlank()
                && privateKey != null && !privateKey.isBlank()
                && subject != null && !subject.isBlank();
    }
}
