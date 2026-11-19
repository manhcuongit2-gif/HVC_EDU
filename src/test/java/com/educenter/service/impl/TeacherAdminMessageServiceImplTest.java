package com.educenter.service.impl;

import com.educenter.entity.Role;
import com.educenter.entity.Teacher;
import com.educenter.entity.TeacherAdminConversation;
import com.educenter.entity.TeacherAdminMessage;
import com.educenter.entity.User;
import com.educenter.repository.TeacherAdminConversationRepository;
import com.educenter.repository.TeacherAdminMessageRepository;
import com.educenter.repository.TeacherRepository;
import com.educenter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeacherAdminMessageServiceImplTest {

    private TeacherAdminConversationRepository conversationRepository;
    private TeacherAdminMessageRepository messageRepository;
    private TeacherRepository teacherRepository;
    private UserRepository userRepository;
    private TeacherAdminMessageServiceImpl service;
    private User adminUser;
    private User teacherUser;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(TeacherAdminConversationRepository.class);
        messageRepository = mock(TeacherAdminMessageRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        userRepository = mock(UserRepository.class);
        service = new TeacherAdminMessageServiceImpl(conversationRepository, messageRepository,
                teacherRepository, userRepository);

        adminUser = user(1L, "ROLE_ADMIN");
        teacherUser = user(2L, "ROLE_TEACHER");
        teacher = new Teacher();
        teacher.setUser(teacherUser);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(teacherRepository.findByUserId(2L)).thenReturn(Optional.of(teacher));
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacherUser));
        when(conversationRepository.findByTeacherUserIdAndAdminUserId(2L, 1L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(TeacherAdminConversation.class))).thenAnswer(invocation -> {
            TeacherAdminConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) conversation.setId(51L);
            return conversation;
        });
        when(messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(51L)).thenReturn(List.of());
    }

    @Test
    void teacherOpensPrivateConversationWithSeededAdmin() {
        var page = service.openForTeacher(2L);

        assertEquals(2L, page.conversation().getTeacherUser().getId());
        assertEquals(1L, page.conversation().getAdminUser().getId());
        assertTrue(page.messages().isEmpty());
        verify(conversationRepository).save(any(TeacherAdminConversation.class));
    }

    @Test
    void onlySeededAdminCanOpenTeacherConversation() {
        User otherAdmin = user(3L, "ROLE_ADMIN");

        assertThrows(IllegalArgumentException.class, () -> service.openForAdmin(otherAdmin.getId(), 2L));

        verifyNoInteractions(conversationRepository);
    }

    @Test
    void openingConversationRequiresActiveTeacherRole() {
        teacherUser.setRole(new Role(2, "ROLE_PARENT", "Parent"));

        assertThrows(IllegalArgumentException.class, () -> service.openForTeacher(2L));

        verifyNoInteractions(conversationRepository);
    }

    @Test
    void teacherMessageIsTrimmedAndPersistedWithTeacherAsSender() {
        service.sendAsTeacher(2L, "  Xin chào admin  ");

        var captor = org.mockito.ArgumentCaptor.forClass(TeacherAdminMessage.class);
        verify(messageRepository).save(captor.capture());
        assertEquals("Xin chào admin", captor.getValue().getContent());
        assertEquals(2L, captor.getValue().getSender().getId());
        assertEquals(1L, captor.getValue().getConversation().getAdminUser().getId());
    }

    @Test
    void rejectsBlankAndOverlongMessages() {
        assertThrows(IllegalArgumentException.class, () -> service.sendAsTeacher(2L, "  "));
        assertThrows(IllegalArgumentException.class, () -> service.sendAsTeacher(2L, "x".repeat(2001)));

        verify(messageRepository, never()).save(any(TeacherAdminMessage.class));
    }

    private User user(Long id, String roleName) {
        User user = new User();
        user.setId(id);
        user.setUsername(roleName.equals("ROLE_ADMIN") ? "admin" : "teacher");
        user.setFullName("Test user " + id);
        user.setActive(true);
        user.setRole(new Role(id.intValue(), roleName, roleName));
        return user;
    }
}
