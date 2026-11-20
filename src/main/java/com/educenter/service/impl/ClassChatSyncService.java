package com.educenter.service.impl;

import com.educenter.entity.ChatConversation;
import com.educenter.entity.ChatMember;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.User;
import com.educenter.repository.ChatConversationRepository;
import com.educenter.repository.ChatMemberRepository;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassStudentRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

@Service
public class ClassChatSyncService {
    private final ClassRepository classes;
    private final ClassStudentRepository enrollments;
    private final ChatConversationRepository conversations;
    private final ChatMemberRepository members;

    public ClassChatSyncService(ClassRepository classes, ClassStudentRepository enrollments,
                                ChatConversationRepository conversations, ChatMemberRepository members) {
        this.classes = classes; this.enrollments = enrollments; this.conversations = conversations; this.members = members;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncOnStartup() { syncClassGroups(); }

    @Scheduled(fixedDelayString = "${app.chat.roster-sync-interval-ms:60000}", initialDelayString = "${app.chat.roster-sync-initial-delay-ms:60000}")
    @Transactional
    public void syncClassGroups() {
        for (ClassEntity classEntity : classes.findAll()) {
            if (!"ACTIVE".equalsIgnoreCase(classEntity.getStatus()) || classEntity.getTeacher() == null
                    || classEntity.getTeacher().getUser() == null) {
                conversations.findByClassEntityId(classEntity.getId()).ifPresent(conversation ->
                        members.findByConversationIdOrderByJoinedAtAsc(conversation.getId()).forEach(members::delete));
                continue;
            }
            ChatConversation conversation = conversations.findByClassEntityId(classEntity.getId()).orElseGet(() -> {
                ChatConversation created = new ChatConversation();
                created.setConversationType("CLASS"); created.setTitle(classEntity.getName() + " · Nhóm lớp");
                created.setClassEntity(classEntity); created.setCreatedBy(classEntity.getTeacher().getUser());
                return conversations.save(created);
            });
            Map<Long, User> currentRoster = new LinkedHashMap<>();
            currentRoster.put(classEntity.getTeacher().getUser().getId(), classEntity.getTeacher().getUser());
            if (classEntity.getAssistantTeacher() != null && classEntity.getAssistantTeacher().getUser() != null)
                currentRoster.put(classEntity.getAssistantTeacher().getUser().getId(), classEntity.getAssistantTeacher().getUser());
            enrollments.findByClassEntityIdAndStatus(classEntity.getId(), "ACTIVE").forEach(enrollment -> {
                if (enrollment.getStudent() != null && enrollment.getStudent().getParent() != null
                        && enrollment.getStudent().getParent().getUser() != null) {
                    User parent = enrollment.getStudent().getParent().getUser();
                    if (Boolean.TRUE.equals(parent.getActive())) currentRoster.put(parent.getId(), parent);
                }
            });
            LinkedHashSet<Long> desiredIds = new LinkedHashSet<>(currentRoster.keySet());
            for (ChatMember existing : members.findByConversationIdOrderByJoinedAtAsc(conversation.getId())) {
                Long userId = existing.getUser().getId();
                if (!desiredIds.contains(userId)) members.delete(existing);
                else { existing.setArchived(false); existing.setNotificationsEnabled(true); desiredIds.remove(userId); }
            }
            for (Long userId : desiredIds) {
                ChatMember member = new ChatMember(); member.setConversation(conversation); member.setUser(currentRoster.get(userId));
                members.save(member);
            }
        }
    }
}
