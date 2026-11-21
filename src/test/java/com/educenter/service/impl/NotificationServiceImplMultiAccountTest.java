package com.educenter.service.impl;

import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.PushSubscriptionOwnershipException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceImplMultiAccountTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserNotificationRepository notifications = mock(UserNotificationRepository.class);
    private final NotificationOutboxRepository outbox = mock(NotificationOutboxRepository.class);
    private final NotificationPreferenceRepository preferences = mock(NotificationPreferenceRepository.class);
    private final PushSubscriptionRepository subscriptions = mock(PushSubscriptionRepository.class);
    private final PushSubscriptionUserRepository links = mock(PushSubscriptionUserRepository.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final NotificationServiceImpl service = new NotificationServiceImpl(users, notifications, outbox,
            preferences, subscriptions, links, messaging);

    @Test
    void explicitOptInsSharePhysicalEndpointAndRemovingOneAccountKeepsTheOther() {
        User first = user(1L);
        User second = user(2L);
        PushSubscription physical = new PushSubscription();
        physical.setUser(first);
        physical.setEndpoint("https://fcm.googleapis.com/device-token");
        physical.setEndpointHash("already-registered");
        physical.setP256dhKey("p256dh");
        physical.setAuthKey("auth");
        physical.setActive(true);
        setId(physical, 55L);
        PushSubscriptionUser firstLink = link(first, physical, 101L);
        PushSubscriptionUser secondLink = link(second, physical, 102L);

        when(users.findById(1L)).thenReturn(Optional.of(first));
        when(users.findById(2L)).thenReturn(Optional.of(second));
        when(subscriptions.findByEndpointHash(anyString())).thenReturn(Optional.of(physical));
        when(links.findBySubscriptionIdAndUserId(55L, 1L)).thenReturn(Optional.of(firstLink));
        when(links.findBySubscriptionIdAndUserId(55L, 2L)).thenReturn(Optional.of(secondLink));
        when(preferences.findByUserId(anyLong())).thenReturn(Optional.of(new NotificationPreference()));
        when(links.findByIdAndUserIdAndActiveTrue(101L, 1L)).thenReturn(Optional.of(firstLink));

        service.saveSubscription(1L, physical.getEndpoint(), "p256dh", "auth", "Laptop", "UA", "Web");
        service.saveSubscription(2L, physical.getEndpoint(), "p256dh", "auth", "Laptop", "UA", "Web");
        service.removeSubscription(1L, 101L);

        assertFalse(firstLink.isActive());
        assertTrue(secondLink.isActive());
        assertSame(first, physical.getUser(), "The original endpoint owner must never be overwritten");
        assertTrue(physical.isActive());
    }

    @Test
    void rejectsEndpointKeyMismatchInsteadOfClaimingExistingEndpoint() {
        User user = user(3L);
        PushSubscription physical = new PushSubscription();
        physical.setEndpoint("https://fcm.googleapis.com/device-token");
        physical.setEndpointHash("hash");
        physical.setP256dhKey("original-key");
        physical.setAuthKey("auth");
        setId(physical, 56L);
        when(users.findById(3L)).thenReturn(Optional.of(user));
        when(subscriptions.findByEndpointHash(anyString())).thenReturn(Optional.of(physical));

        assertThrows(PushSubscriptionOwnershipException.class, () -> service.saveSubscription(3L,
                physical.getEndpoint(), "different-key", "auth", "Laptop", "UA", "Web"));
        verify(links, never()).saveAndFlush(any());
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private PushSubscriptionUser link(User user, PushSubscription subscription, Long id) {
        PushSubscriptionUser link = new PushSubscriptionUser();
        link.setUser(user);
        link.setSubscription(subscription);
        setId(link, id);
        return link;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
