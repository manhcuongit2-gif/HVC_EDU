package com.educenter.service.impl;

import com.educenter.config.PushProperties;
import com.educenter.entity.NotificationDelivery;
import com.educenter.entity.NotificationOutbox;
import com.educenter.entity.PushSubscription;
import com.educenter.repository.NotificationDeliveryRepository;
import com.educenter.repository.NotificationOutboxRepository;
import com.educenter.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxDispatcher.class);
    private static final int MAX_ATTEMPTS = 5;
    private final NotificationOutboxRepository outbox;
    private final PushSubscriptionRepository subscriptions;
    private final NotificationDeliveryRepository deliveries;
    private final PushProperties properties;
    private final ObjectMapper objectMapper;
    private final ActiveChatPresence activeChatPresence;

    public NotificationOutboxDispatcher(NotificationOutboxRepository outbox, PushSubscriptionRepository subscriptions,
                                         NotificationDeliveryRepository deliveries, PushProperties properties,
                                         ObjectMapper objectMapper, ActiveChatPresence activeChatPresence) {
        this.outbox = outbox;
        this.subscriptions = subscriptions;
        this.deliveries = deliveries;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.activeChatPresence = activeChatPresence;
    }

    @Scheduled(fixedDelayString = "${app.push.dispatch-interval-ms:15000}")
    @Transactional
    public void dispatchDue() {
        if (!properties.configured()) return;
        LocalDateTime now = LocalDateTime.now();
        List<NotificationOutbox> due = new java.util.ArrayList<>(outbox
                .findTop100ByStatusAndScheduledAtLessThanEqualAndNextAttemptAtIsNullOrderByScheduledAtAsc("PENDING", now));
        due.addAll(outbox.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc("RETRY", now));
        for (NotificationOutbox job : due) dispatch(job);
    }

    private void dispatch(NotificationOutbox job) {
        if (("MESSAGE_PRIVATE".equals(job.getCategory()) || "MESSAGE_GROUP".equals(job.getCategory()))
                && job.getTargetPath() != null && job.getTargetPath().matches("/chat/[0-9]+")) {
            Long conversationId = Long.parseLong(job.getTargetPath().substring("/chat/".length()));
            if (activeChatPresence.viewing(job.getUser().getId(), conversationId)) { job.setStatus("SUPPRESSED_ACTIVE"); return; }
        }
        List<PushSubscription> targets = subscriptions.findActiveForLinkedUser(job.getUser().getId());
        if (targets.isEmpty()) { job.setStatus("NO_DEVICE"); return; }
        boolean hasFailure = false;
        String payload;
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            // Sanitize legacy outbox rows too: queued notifications may predate lock-screen privacy.
            data.put("title", PushNotificationContent.title(job.getCategory()));
            data.put("body", PushNotificationContent.body(job.getCategory()));
            data.put("url", job.getTargetPath()); data.put("tag", "educenter-" + job.getEventKey());
            data.put("silent", !job.isSoundEnabled());
            payload = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            job.setStatus("FAILED"); job.setLastError("Could not encode push payload"); return;
        }
        for (PushSubscription target : targets) {
            NotificationDelivery delivery = deliveries.findByOutboxIdAndSubscriptionId(job.getId(), target.getId())
                    .orElseGet(() -> { NotificationDelivery d = new NotificationDelivery(); d.setOutbox(job); d.setSubscription(target); return d; });
            if ("DELIVERED".equals(delivery.getStatus())) continue;
            try {
                PushService push = new PushService(properties.publicKey(), properties.privateKey(), properties.subject());
                HttpResponse response = push.send(new Notification(target.getEndpoint(), target.getP256dhKey(), target.getAuthKey(), payload));
                int status = response.getStatusLine().getStatusCode();
                delivery.setAttemptCount(delivery.getAttemptCount() + 1);
                if (status >= 200 && status < 300) {
                    delivery.setStatus("DELIVERED"); delivery.setAcceptedAt(LocalDateTime.now()); delivery.setLastError(null);
                } else {
                    delivery.setStatus("FAILED"); delivery.setLastError("Push provider returned HTTP " + status); hasFailure = true;
                    if (status == 404 || status == 410) target.setActive(false);
                }
            } catch (Exception e) {
                delivery.setAttemptCount(delivery.getAttemptCount() + 1);
                delivery.setStatus("FAILED"); delivery.setLastError(shortError(e)); hasFailure = true;
                log.warn("Web Push delivery failed for subscription {}: {}", target.getId(), shortError(e));
            }
            deliveries.save(delivery);
        }
        job.setAttemptCount(job.getAttemptCount() + 1);
        if (!hasFailure) {
            job.setStatus("DELIVERED"); job.setAcceptedAt(LocalDateTime.now()); job.setLastError(null); job.setNextAttemptAt(null);
        } else if (job.getAttemptCount() >= MAX_ATTEMPTS) {
            job.setStatus("FAILED");
        } else {
            job.setStatus("RETRY");
            job.setNextAttemptAt(LocalDateTime.now().plusSeconds(Math.min(3600, 30L << job.getAttemptCount())));
        }
    }

    private String shortError(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
