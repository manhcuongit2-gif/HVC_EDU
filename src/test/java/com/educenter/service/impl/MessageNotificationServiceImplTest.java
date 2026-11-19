package com.educenter.service.impl;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.ParentTeacherConversation;
import com.educenter.entity.ParentTeacherMessage;
import com.educenter.entity.TeacherAdminConversation;
import com.educenter.entity.TeacherAdminMessage;
import com.educenter.entity.User;
import com.educenter.repository.ParentTeacherConversationRepository;
import com.educenter.repository.ParentTeacherMessageRepository;
import com.educenter.repository.TeacherAdminConversationRepository;
import com.educenter.repository.TeacherAdminMessageRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageNotificationServiceImplTest {

    private final ParentTeacherConversationRepository parentConversations = mock(ParentTeacherConversationRepository.class);
    private final ParentTeacherMessageRepository parentMessages = mock(ParentTeacherMessageRepository.class);
    private final TeacherAdminConversationRepository adminConversations = mock(TeacherAdminConversationRepository.class);
    private final TeacherAdminMessageRepository adminMessages = mock(TeacherAdminMessageRepository.class);
    private final MessageNotificationServiceImpl service = new MessageNotificationServiceImpl(
            parentConversations, parentMessages, adminConversations, adminMessages);

    @Test
    void parentGetsUnreadSenderAndConversationLink() {
        ParentTeacherConversation conversation = new ParentTeacherConversation();
        conversation.setId(20L);
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(7L);
        conversation.setClassEntity(classEntity);
        conversation.setParentLastReadMessageId(0L);

        User teacher = user(2L, "Cô giáo");
        ParentTeacherMessage message = new ParentTeacherMessage();
        message.setId(4L);
        message.setSender(teacher);
        message.setContent("  Bài tập đã được giao  ");

        when(parentConversations.findByParentUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(conversation));
        when(parentMessages.findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(20L, 0L, 1L))
                .thenReturn(List.of(message));

        var summary = service.getUnreadFor(1L, "ROLE_PARENT");

        assertEquals(1, summary.unreadCount());
        assertEquals("Cô giáo", summary.items().getFirst().senderName());
        assertEquals("Bài tập đã được giao", summary.items().getFirst().preview());
        assertEquals("/parent/messages/7", summary.items().getFirst().conversationUrl());
    }

    @Test
    void teacherGetsNotificationsFromParentAndAdmin() {
        ParentTeacherConversation parentConversation = new ParentTeacherConversation();
        parentConversation.setId(21L);
        parentConversation.setClassEntity(classEntity(8L));
        parentConversation.setParent(parent(3L, user(30L, "Phụ huynh")));
        TeacherAdminConversation adminConversation = new TeacherAdminConversation();
        adminConversation.setId(22L);
        adminConversation.setAdminUser(user(1L, "Quản trị viên"));

        ParentTeacherMessage parentMessage = new ParentTeacherMessage();
        parentMessage.setId(10L);
        parentMessage.setSender(user(30L, "Phụ huynh"));
        parentMessage.setContent("Xin phép nghỉ buổi học");
        TeacherAdminMessage adminMessage = new TeacherAdminMessage();
        adminMessage.setId(11L);
        adminMessage.setSender(user(1L, "Quản trị viên"));
        adminMessage.setContent("Đã nhận thông tin");
        adminMessage.setCreatedAt(LocalDateTime.of(2026, 11, 19, 10, 0));

        when(parentConversations.findForTeacher(2L)).thenReturn(List.of(parentConversation));
        when(parentMessages.findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(21L, 0L, 2L))
                .thenReturn(List.of(parentMessage));
        when(adminConversations.findByTeacherUserIdOrderByUpdatedAtDesc(2L)).thenReturn(List.of(adminConversation));
        when(adminMessages.findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(22L, 0L, 2L))
                .thenReturn(List.of(adminMessage));

        var summary = service.getUnreadFor(2L, "ROLE_TEACHER");

        assertEquals(2, summary.unreadCount());
        assertEquals("Quản trị viên", summary.items().getFirst().senderName());
        assertEquals("/teacher/admin-messages", summary.items().getFirst().conversationUrl());
        assertEquals("/teacher/messages/8/3", summary.items().get(1).conversationUrl());
    }

    @Test
    void adminGetsTeacherUnreadMessage() {
        TeacherAdminConversation conversation = new TeacherAdminConversation();
        conversation.setId(25L);
        conversation.setTeacherUser(user(2L, "Giáo viên Toán"));
        TeacherAdminMessage message = new TeacherAdminMessage();
        message.setId(13L);
        message.setSender(conversation.getTeacherUser());
        message.setContent("Xin đổi lịch dạy");
        message.setCreatedAt(LocalDateTime.of(2026, 11, 19, 12, 0));

        when(adminConversations.findByAdminUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(conversation));
        when(adminMessages.findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(25L, 0L, 1L))
                .thenReturn(List.of(message));

        var summary = service.getUnreadFor(1L, "ROLE_ADMIN");

        assertEquals(1, summary.unreadCount());
        assertEquals("Giáo viên Toán", summary.items().getFirst().senderName());
        assertEquals("/admin/messages/2", summary.items().getFirst().conversationUrl());
    }

    private User user(Long id, String fullName) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        return user;
    }

    private ClassEntity classEntity(Long id) {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(id);
        return classEntity;
    }

    private com.educenter.entity.Parent parent(Long id, User user) {
        com.educenter.entity.Parent parent = new com.educenter.entity.Parent();
        parent.setId(id);
        parent.setUser(user);
        return parent;
    }
}
