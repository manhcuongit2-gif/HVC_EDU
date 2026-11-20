package com.educenter.security;

import com.educenter.repository.ChatMemberRepository;
import com.educenter.repository.ParentTeacherConversationRepository;
import com.educenter.repository.TeacherAdminConversationRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class ChatSubscriptionInterceptor implements ChannelInterceptor {
    private final ChatMemberRepository members;
    private final ParentTeacherConversationRepository parentTeacherConversations;
    private final TeacherAdminConversationRepository teacherAdminConversations;

    public ChatSubscriptionInterceptor(ChatMemberRepository members,
                                       ParentTeacherConversationRepository parentTeacherConversations,
                                       TeacherAdminConversationRepository teacherAdminConversations) {
        this.members = members;
        this.parentTeacherConversations = parentTeacherConversations;
        this.teacherAdminConversations = teacherAdminConversations;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && (destination.startsWith("/topic/") || destination.startsWith("/queue/") || destination.startsWith("/user/")))
                throw new AccessDeniedException("Clients cannot publish directly to broker destinations.");
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/chat/")) {
                Long conversationId = parseId(destination.substring("/topic/chat/".length()));
                Long userId = authenticatedUserId(accessor.getUser());
                if (conversationId == null || userId == null || !members.existsByConversationIdAndUserId(conversationId, userId))
                    throw new AccessDeniedException("You are not a member of this chat conversation.");
            }
            if (destination != null && destination.startsWith("/topic/parent-teacher/")) {
                Long conversationId = parseId(destination.substring("/topic/parent-teacher/".length()));
                Long userId = authenticatedUserId(accessor.getUser());
                boolean allowed = conversationId != null && userId != null && parentTeacherConversations.findById(conversationId)
                        .map(conversation -> {
                            if (conversation.getParent() != null && conversation.getParent().getUser() != null
                                    && userId.equals(conversation.getParent().getUser().getId())) return true;
                            var classEntity = conversation.getClassEntity();
                            return classEntity != null && ((classEntity.getTeacher() != null && classEntity.getTeacher().getUser() != null
                                    && userId.equals(classEntity.getTeacher().getUser().getId()))
                                    || (classEntity.getAssistantTeacher() != null && classEntity.getAssistantTeacher().getUser() != null
                                    && userId.equals(classEntity.getAssistantTeacher().getUser().getId())));
                        }).orElse(false);
                if (!allowed) throw new AccessDeniedException("You are not a member of this parent-teacher conversation.");
            }
            if (destination != null && destination.startsWith("/topic/teacher-admin/")) {
                Long conversationId = parseId(destination.substring("/topic/teacher-admin/".length()));
                Long userId = authenticatedUserId(accessor.getUser());
                boolean allowed = conversationId != null && userId != null && teacherAdminConversations.findById(conversationId)
                        .map(conversation -> userId.equals(conversation.getTeacherUser().getId())
                                || userId.equals(conversation.getAdminUser().getId())).orElse(false);
                if (!allowed) throw new AccessDeniedException("You are not a member of this teacher-admin conversation.");
            }
        }
        return message;
    }

    private Long parseId(String value) {
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return null; }
    }
    private Long authenticatedUserId(Principal principal) {
        if (principal instanceof Authentication authentication && authentication.getPrincipal() instanceof CustomUserDetails user) return user.getId();
        return null;
    }
}
