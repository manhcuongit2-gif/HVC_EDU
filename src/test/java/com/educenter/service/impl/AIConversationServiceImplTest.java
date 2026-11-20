package com.educenter.service.impl;

import com.educenter.entity.AIConversation;
import com.educenter.repository.AIConversationRepository;
import com.educenter.repository.AIMessageRepository;
import com.educenter.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AIConversationServiceImplTest {
    @Test
    void refusesToReuseConversationForAnotherChild() {
        AIConversation conversation = new AIConversation();
        conversation.setContextRole("ROLE_PARENT");
        conversation.setContextId(101L);
        AIConversationRepository conversations = mock(AIConversationRepository.class);
        AIMessageRepository messages = mock(AIMessageRepository.class);
        when(conversations.findByIdAndOwnerIdForUpdate(9L, 7L)).thenReturn(Optional.of(conversation));
        when(messages.countByConversationId(9L)).thenReturn(2L);
        AIConversationServiceImpl service = new AIConversationServiceImpl(conversations, messages, mock(UserRepository.class));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.bindContext(9L, 7L, "ROLE_PARENT", 202L));

        assertTrue(error.getMessage().contains("Phạm vi dữ liệu"));
        verify(conversations, never()).save(any());
    }

    @Test
    void bindsAnEmptyConversationToItsFirstContext() {
        AIConversation conversation = new AIConversation();
        AIConversationRepository conversations = mock(AIConversationRepository.class);
        AIMessageRepository messages = mock(AIMessageRepository.class);
        when(conversations.findByIdAndOwnerIdForUpdate(9L, 7L)).thenReturn(Optional.of(conversation));
        when(conversations.save(conversation)).thenReturn(conversation);
        when(messages.countByConversationId(9L)).thenReturn(0L);
        AIConversationServiceImpl service = new AIConversationServiceImpl(conversations, messages, mock(UserRepository.class));

        AIConversation result = service.bindContext(9L, 7L, "ROLE_PARENT", 202L);

        assertSame(conversation, result);
        assertEquals("ROLE_PARENT", result.getContextRole());
        assertEquals(202L, result.getContextId());
    }
}
