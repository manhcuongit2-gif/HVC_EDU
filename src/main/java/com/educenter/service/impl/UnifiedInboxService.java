package com.educenter.service.impl;

import com.educenter.dto.ChatConversationSummary;
import com.educenter.dto.ConversationTarget;
import com.educenter.dto.InboxContactView;
import com.educenter.dto.InboxConversationView;
import com.educenter.dto.InboxTabView;
import com.educenter.entity.ChatConversation;
import com.educenter.entity.ChatMember;
import com.educenter.entity.ChatMessage;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ParentTeacherConversation;
import com.educenter.entity.ParentTeacherMessage;
import com.educenter.entity.Teacher;
import com.educenter.entity.TeacherAdminConversation;
import com.educenter.entity.TeacherAdminMessage;
import com.educenter.entity.User;
import com.educenter.repository.ChatMemberRepository;
import com.educenter.repository.ParentTeacherConversationRepository;
import com.educenter.repository.ParentTeacherMessageRepository;
import com.educenter.repository.TeacherAdminConversationRepository;
import com.educenter.repository.TeacherAdminMessageRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.ChatService;
import com.educenter.service.ParentTeacherMessageService;
import com.educenter.service.TeacherAdminMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UnifiedInboxService {
    private static final String PARENT = "ROLE_PARENT";
    private static final String TEACHER = "ROLE_TEACHER";
    private static final String ADMIN = "ROLE_ADMIN";

    private final ChatService chats;
    private final ChatMemberRepository chatMembers;
    private final ParentTeacherConversationRepository parentTeacherConversations;
    private final ParentTeacherMessageRepository parentTeacherMessages;
    private final TeacherAdminConversationRepository teacherAdminConversations;
    private final TeacherAdminMessageRepository teacherAdminMessages;
    private final ParentTeacherMessageService parentTeacherService;
    private final TeacherAdminMessageService teacherAdminService;
    private final UserRepository users;

    public UnifiedInboxService(ChatService chats, ChatMemberRepository chatMembers,
                               ParentTeacherConversationRepository parentTeacherConversations,
                               ParentTeacherMessageRepository parentTeacherMessages,
                               TeacherAdminConversationRepository teacherAdminConversations,
                               TeacherAdminMessageRepository teacherAdminMessages,
                               ParentTeacherMessageService parentTeacherService,
                               TeacherAdminMessageService teacherAdminService,
                               UserRepository users) {
        this.chats = chats;
        this.chatMembers = chatMembers;
        this.parentTeacherConversations = parentTeacherConversations;
        this.parentTeacherMessages = parentTeacherMessages;
        this.teacherAdminConversations = teacherAdminConversations;
        this.teacherAdminMessages = teacherAdminMessages;
        this.parentTeacherService = parentTeacherService;
        this.teacherAdminService = teacherAdminService;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<InboxConversationView> conversations(Long userId, String role, String search, String tab) {
        List<InboxConversationView> items = new ArrayList<>();
        if (PARENT.equals(role)) addParentTeacherConversations(items, userId);
        if (TEACHER.equals(role)) {
            addTeacherParentConversations(items, userId);
            addTeacherAdminConversations(items, userId);
        }
        if (ADMIN.equals(role)) addAdminTeacherConversations(items, userId);
        addGenericConversations(items, userId);

        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Set<String> genericMatches = needle.isEmpty() ? Set.of() : chats.findConversations(userId, needle).stream()
                .map(summary -> "/chat/" + summary.conversation().getId()).collect(Collectors.toSet());
        return items.stream()
                .filter(item -> matchesTab(item, tab))
                .filter(item -> needle.isEmpty() || genericMatches.contains(item.url()) || contains(item.title(), needle)
                        || contains(item.subtitle(), needle) || contains(item.preview(), needle))
                .sorted(Comparator.comparing(InboxConversationView::updatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InboxContactView> contacts(Long userId, String role) {
        List<InboxContactView> contacts = new ArrayList<>();
        if (PARENT.equals(role)) {
            users.findByUsername("admin")
                    .filter(user -> Boolean.TRUE.equals(user.getActive()) && user.getRole() != null
                            && ADMIN.equals(user.getRole().getName()))
                    .ifPresent(admin -> contacts.add(contact("admin", "Quản trị viên", admin.getFullName(), null, admin.getId())));
            for (ClassEntity classEntity : parentTeacherService.findParentClasses(userId)) {
                Teacher teacher = classEntity.getTeacher();
                String teacherName = teacher == null || teacher.getUser() == null
                        ? "Người dạy phụ trách" : teacher.getUser().getFullName();
                contacts.add(contact("teacher", teacherName, classEntity.getName(),
                        "/parent/messages/" + classEntity.getId(), null));
            }
        } else if (TEACHER.equals(role)) {
            for (ConversationTarget target : parentTeacherService.findTeacherTargets(userId)) {
                contacts.add(contact("parent", target.parent().getUser().getFullName(),
                        target.classEntity().getName() + " · " + String.join(", ", target.studentNames()),
                        "/teacher/messages/" + target.classEntity().getId() + "/" + target.parent().getId(), null));
            }
            contacts.add(contact("admin", "Quản trị viên", "Trao đổi riêng với quản trị viên",
                    "/teacher/admin-messages", null));
            for (User user : chats.findContacts(userId, role)) {
                contacts.add(contact("teacher", user.getFullName(), "Người dạy", null, user.getId()));
            }
        } else if (ADMIN.equals(role)) {
            for (User user : users.findByRole_NameAndActiveTrueOrderByFullNameAsc(PARENT)) {
                contacts.add(contact("parent", user.getFullName(), "Phụ huynh", null, user.getId()));
            }
            try {
                for (Teacher teacher : teacherAdminService.findTeachersForAdmin(userId)) {
                    contacts.add(contact("teacher", teacher.getUser().getFullName(), "Trao đổi với người dạy",
                            "/admin/messages/" + teacher.getUser().getId(), null));
                }
            } catch (IllegalArgumentException ignored) {
                // Legacy teacher-admin conversations are available to the primary admin account.
            }
        }
        return contacts;
    }

    public List<InboxTabView> tabs(String role) {
        List<InboxTabView> tabs = new ArrayList<>();
        tabs.add(new InboxTabView("all", "Tất cả"));
        if (PARENT.equals(role)) {
            tabs.add(new InboxTabView("teacher", "Người dạy"));
            tabs.add(new InboxTabView("admin", "Admin"));
            tabs.add(new InboxTabView("groups", "Nhóm lớp & tin nhắn cũ"));
        } else if (TEACHER.equals(role)) {
            tabs.add(new InboxTabView("parent", "Phụ huynh"));
            tabs.add(new InboxTabView("admin", "Admin"));
            tabs.add(new InboxTabView("teacher", "Người dạy"));
            tabs.add(new InboxTabView("groups", "Nhóm & tin nhắn cũ"));
        } else if (ADMIN.equals(role)) {
            tabs.add(new InboxTabView("parent", "Phụ huynh"));
            tabs.add(new InboxTabView("teacher", "Người dạy"));
            tabs.add(new InboxTabView("groups", "Nhóm & tin nhắn cũ"));
        }
        return tabs;
    }

    public String normalizeTab(String requested, String role) {
        return tabs(role).stream().anyMatch(tab -> tab.key().equals(requested)) ? requested : "all";
    }

    private void addParentTeacherConversations(List<InboxConversationView> items, Long userId) {
        for (ParentTeacherConversation conversation : parentTeacherConversations.findByParentUserIdOrderByUpdatedAtDesc(userId)) {
            ParentTeacherMessage latest = parentTeacherMessages.findTopByConversationIdOrderByIdDesc(conversation.getId()).orElse(null);
            long unread = parentTeacherMessages.findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(
                    conversation.getId(), conversation.getParentLastReadMessageId(), userId).size();
            ClassEntity classEntity = conversation.getClassEntity();
            Teacher teacher = classEntity.getTeacher();
            String title = teacher == null || teacher.getUser() == null ? "Người dạy" : teacher.getUser().getFullName();
            items.add(new InboxConversationView("teacher", "Người dạy", title, classEntity.getName(),
                    latest == null ? "Chưa có tin nhắn" : latest.getContent(), initials(title),
                    "/parent/messages/" + classEntity.getId(), latest == null ? conversation.getUpdatedAt() : latest.getCreatedAt(), unread, false));
        }
    }

    private void addTeacherParentConversations(List<InboxConversationView> items, Long userId) {
        for (ParentTeacherConversation conversation : parentTeacherConversations.findForTeacher(userId)) {
            ParentTeacherMessage latest = parentTeacherMessages.findTopByConversationIdOrderByIdDesc(conversation.getId()).orElse(null);
            long unread = parentTeacherMessages.findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(
                    conversation.getId(), conversation.getTeacherLastReadMessageId(), userId).size();
            String title = conversation.getParent().getUser().getFullName();
            String className = conversation.getClassEntity().getName();
            items.add(new InboxConversationView("parent", "Phụ huynh", title, className,
                    latest == null ? "Chưa có tin nhắn" : latest.getContent(), initials(title),
                    "/teacher/messages/" + conversation.getClassEntity().getId() + "/" + conversation.getParent().getId(),
                    latest == null ? conversation.getUpdatedAt() : latest.getCreatedAt(), unread, false));
        }
    }

    private void addTeacherAdminConversations(List<InboxConversationView> items, Long userId) {
        for (TeacherAdminConversation conversation : teacherAdminConversations.findByTeacherUserIdOrderByUpdatedAtDesc(userId)) {
            TeacherAdminMessage latest = teacherAdminMessages.findTopByConversationIdOrderByIdDesc(conversation.getId()).orElse(null);
            long unread = teacherAdminMessages.findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(
                    conversation.getId(), conversation.getTeacherLastReadMessageId(), userId).size();
            String title = conversation.getAdminUser().getFullName();
            items.add(new InboxConversationView("admin", "Admin", title, "Trao đổi với quản trị viên",
                    latest == null ? "Chưa có tin nhắn" : latest.getContent(), initials(title), "/teacher/admin-messages",
                    latest == null ? conversation.getUpdatedAt() : latest.getCreatedAt(), unread, false));
        }
    }

    private void addAdminTeacherConversations(List<InboxConversationView> items, Long userId) {
        for (TeacherAdminConversation conversation : teacherAdminConversations.findByAdminUserIdOrderByUpdatedAtDesc(userId)) {
            TeacherAdminMessage latest = teacherAdminMessages.findTopByConversationIdOrderByIdDesc(conversation.getId()).orElse(null);
            long unread = teacherAdminMessages.findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(
                    conversation.getId(), conversation.getAdminLastReadMessageId(), userId).size();
            String title = conversation.getTeacherUser().getFullName();
            items.add(new InboxConversationView("teacher", "Người dạy", title, "Trao đổi riêng với người dạy",
                    latest == null ? "Chưa có tin nhắn" : latest.getContent(), initials(title),
                    "/admin/messages/" + conversation.getTeacherUser().getId(),
                    latest == null ? conversation.getUpdatedAt() : latest.getCreatedAt(), unread, false));
        }
    }

    private void addGenericConversations(List<InboxConversationView> items, Long userId) {
        for (ChatConversationSummary summary : chats.findConversations(userId, null)) {
            ChatConversation conversation = summary.conversation();
            List<ChatMember> members = chatMembers.findByConversationIdOrderByJoinedAtAsc(conversation.getId());
            boolean group = conversation.getClassEntity() != null || members.size() > 2
                    || "CLASS".equals(conversation.getConversationType());
            String category = group ? "groups" : directCategory(members, userId);
            String label = group ? "Nhóm lớp" : categoryLabel(category);
            ChatMessage latest = summary.latestMessage();
            String preview = latest == null ? "Chưa có tin nhắn"
                    : latest.isDeletedForAll() ? "Đã thu hồi tin nhắn"
                    : latest.getContent() != null ? latest.getContent()
                    : latest.getOriginalFilename() == null ? "Chưa có tin nhắn" : "Đã gửi tệp: " + latest.getOriginalFilename();
            String subtitle = conversation.getClassEntity() == null ? label : conversation.getClassEntity().getName();
            items.add(new InboxConversationView(category, label, summary.displayTitle(), subtitle, preview,
                    summary.initials(), "/chat/" + conversation.getId(), conversation.getUpdatedAt(), summary.unreadCount(), group));
        }
    }

    private String directCategory(List<ChatMember> members, Long userId) {
        return members.stream().map(ChatMember::getUser).filter(user -> !user.getId().equals(userId))
                .map(User::getRole).filter(java.util.Objects::nonNull).map(roleEntity -> roleEntity.getName())
                .map(otherRole -> switch (otherRole) {
                    case PARENT -> "parent";
                    case TEACHER -> "teacher";
                    case ADMIN -> "admin";
                    default -> "other";
                }).findFirst().orElse("other");
    }

    private String categoryLabel(String category) {
        return switch (category) {
            case "parent" -> "Phụ huynh";
            case "teacher" -> "Người dạy";
            case "admin" -> "Admin";
            default -> "Tin nhắn trực tiếp";
        };
    }

    private InboxContactView contact(String category, String title, String subtitle, String url, Long recipientId) {
        return new InboxContactView(category, title, subtitle, initials(title), url, recipientId);
    }

    private boolean matchesTab(InboxConversationView item, String tab) {
        if (tab == null || tab.isBlank() || "all".equals(tab)) return true;
        if ("groups".equals(tab)) return item.group();
        return tab.equals(item.category());
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "HC";
        String[] words = name.trim().split("\\s+");
        return words.length == 1 ? words[0].substring(0, 1).toUpperCase(Locale.ROOT)
                : (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }
}
