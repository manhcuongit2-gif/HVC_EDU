package com.educenter.dto;

public record InboxContactView(
        String category,
        String title,
        String subtitle,
        String initials,
        String url,
        Long recipientUserId) {
    public boolean startsGenericConversation() { return recipientUserId != null; }
}
