package com.educenter.service.impl;

import com.educenter.repository.UserRepository;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

@Component
public class ActiveChatPresence {
    private final SimpUserRegistry users;
    private final UserRepository userRepository;

    public ActiveChatPresence(SimpUserRegistry users, UserRepository userRepository) {
        this.users = users; this.userRepository = userRepository;
    }

    public boolean viewing(Long userId, Long conversationId) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getUsername() == null) return false;
        var simpUser = users.getUser(user.getUsername());
        if (simpUser == null) return false;
        String destination = "/topic/chat/" + conversationId;
        return simpUser.getSessions().stream().flatMap(session -> session.getSubscriptions().stream())
                .anyMatch(subscription -> destination.equals(subscription.getDestination()));
    }
}
