package com.educenter.service.impl;

import com.educenter.dto.MessageNotification;
import com.educenter.dto.MessageNotificationSummary;
import com.educenter.entity.ParentTeacherConversation;
import com.educenter.entity.ParentTeacherMessage;
import com.educenter.entity.TeacherAdminConversation;
import com.educenter.entity.TeacherAdminMessage;
import com.educenter.entity.ChatMember;
import com.educenter.entity.ChatMessage;
import com.educenter.repository.ChatMemberRepository;
import com.educenter.repository.ChatMessageRepository;
import com.educenter.repository.ParentTeacherConversationRepository;
import com.educenter.repository.ParentTeacherMessageRepository;
import com.educenter.repository.TeacherAdminConversationRepository;
import com.educenter.repository.TeacherAdminMessageRepository;
import com.educenter.service.MessageNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MessageNotificationServiceImpl implements MessageNotificationService {

    private static final int MAX_ITEMS = 10;
    private static final String PARENT_ROLE = "ROLE_PARENT";
    private static final String TEACHER_ROLE = "ROLE_TEACHER";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final ParentTeacherConversationRepository parentConversationRepository;
    private final ParentTeacherMessageRepository parentMessageRepository;
    private final TeacherAdminConversationRepository teacherAdminConversationRepository;
    private final TeacherAdminMessageRepository teacherAdminMessageRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;

    public MessageNotificationServiceImpl(ParentTeacherConversationRepository parentConversationRepository,
                                          ParentTeacherMessageRepository parentMessageRepository,
                                          TeacherAdminConversationRepository teacherAdminConversationRepository,
                                          TeacherAdminMessageRepository teacherAdminMessageRepository,
                                          ChatMemberRepository chatMemberRepository,
                                          ChatMessageRepository chatMessageRepository) {
        this.parentConversationRepository = parentConversationRepository;
        this.parentMessageRepository = parentMessageRepository;
        this.teacherAdminConversationRepository = teacherAdminConversationRepository;
        this.teacherAdminMessageRepository = teacherAdminMessageRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MessageNotificationSummary getUnreadFor(Long userId, String roleName) {
        List<MessageNotification> items = new ArrayList<>();
        long unreadCount = 0;

        if (PARENT_ROLE.equals(roleName)) {
            for (ParentTeacherConversation conversation : parentConversationRepository.findByParentUserIdOrderByUpdatedAtDesc(userId)) {
                List<ParentTeacherMessage> unread = parentMessageRepository
                        .findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(
                                conversation.getId(), conversation.getParentLastReadMessageId(), userId);
                unreadCount += unread.size();
                if (!unread.isEmpty()) {
                    ParentTeacherMessage latest = unread.get(0);
                    items.add(new MessageNotification(latest.getSender().getFullName(), preview(latest.getContent()),
                            latest.getCreatedAt(), "/parent/messages/" + conversation.getClassEntity().getId()));
                }
            }
        } else if (TEACHER_ROLE.equals(roleName)) {
            for (ParentTeacherConversation conversation : parentConversationRepository.findForTeacher(userId)) {
                List<ParentTeacherMessage> unread = parentMessageRepository
                        .findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(
                                conversation.getId(), conversation.getTeacherLastReadMessageId(), userId);
                unreadCount += unread.size();
                if (!unread.isEmpty()) {
                    ParentTeacherMessage latest = unread.get(0);
                    items.add(new MessageNotification(latest.getSender().getFullName(), preview(latest.getContent()),
                            latest.getCreatedAt(), "/teacher/messages/" + conversation.getClassEntity().getId()
                            + "/" + conversation.getParent().getId()));
                }
            }
            for (TeacherAdminConversation conversation : teacherAdminConversationRepository.findByTeacherUserIdOrderByUpdatedAtDesc(userId)) {
                List<TeacherAdminMessage> unread = teacherAdminMessageRepository
                        .findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(
                                conversation.getId(), conversation.getTeacherLastReadMessageId(), userId);
                unreadCount += unread.size();
                if (!unread.isEmpty()) {
                    TeacherAdminMessage latest = unread.get(0);
                    items.add(new MessageNotification(latest.getSender().getFullName(), preview(latest.getContent()),
                            latest.getCreatedAt(), "/teacher/admin-messages"));
                }
            }
        } else if (ADMIN_ROLE.equals(roleName)) {
            for (TeacherAdminConversation conversation : teacherAdminConversationRepository.findByAdminUserIdOrderByUpdatedAtDesc(userId)) {
                List<TeacherAdminMessage> unread = teacherAdminMessageRepository
                        .findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(
                                conversation.getId(), conversation.getAdminLastReadMessageId(), userId);
                unreadCount += unread.size();
                if (!unread.isEmpty()) {
                    TeacherAdminMessage latest = unread.get(0);
                    items.add(new MessageNotification(latest.getSender().getFullName(), preview(latest.getContent()),
                            latest.getCreatedAt(), "/admin/messages/" + conversation.getTeacherUser().getId()));
                }
            }
        }

        for (ChatMember member : chatMemberRepository.findActiveConversationsForUser(userId)) {
            if (!member.isNotificationsEnabled()) continue;
            ChatMessage latestUnread = chatMessageRepository
                    .findByConversationIdAndIdGreaterThanOrderByIdAsc(member.getConversation().getId(), member.getLastReadMessageId())
                    .stream().filter(message -> !message.getSender().getId().equals(userId))
                    .max(Comparator.comparing(ChatMessage::getId)).orElse(null);
            if (latestUnread == null) continue;
            unreadCount += chatMessageRepository.countByConversationIdAndIdGreaterThanAndSenderIdNot(
                    member.getConversation().getId(), member.getLastReadMessageId(), userId);
            String content = latestUnread.isDeletedForAll() ? "Đã thu hồi tin nhắn" : latestUnread.getContent();
            if (content == null && latestUnread.getOriginalFilename() != null) content = "Đã gửi tệp: " + latestUnread.getOriginalFilename();
            items.add(new MessageNotification(latestUnread.getSender().getFullName(), preview(content),
                    latestUnread.getCreatedAt(), "/chat/" + member.getConversation().getId()));
        }

        items.sort(Comparator.comparing(MessageNotification::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<MessageNotification> latestItems = items.stream().limit(MAX_ITEMS).toList();
        return new MessageNotificationSummary(unreadCount, latestItems);
    }

    private String preview(String content) {
        if (content == null) return "";
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 90 ? normalized : normalized.substring(0, 87) + "...";
    }
}
