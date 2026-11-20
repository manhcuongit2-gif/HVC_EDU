package com.educenter.service.impl;

import com.educenter.dto.ChatConversationSummary;
import com.educenter.dto.ChatMessagePayload;
import com.educenter.dto.ChatPage;
import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.ChatService;
import com.educenter.service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatServiceImpl implements ChatService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "jpg", "jpeg", "png", "gif", "webp",
            "mp3", "wav", "ogg", "m4a", "webm", "aac"
    );
    private static final Set<String> ALLOWED_REACTIONS = Set.of("👍", "❤️", "😂", "😮", "😢", "🙏", "🎉");
    private static final long MAX_ATTACHMENT_BYTES = 20L * 1024 * 1024;

    private final ChatConversationRepository conversationRepository;
    private final ChatMemberRepository memberRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Path uploadDirectory;

    public ChatServiceImpl(ChatConversationRepository conversationRepository, ChatMemberRepository memberRepository,
                           ChatMessageRepository messageRepository, ChatMessageReactionRepository reactionRepository,
                           UserRepository userRepository, NotificationService notificationService,
                           SimpMessagingTemplate messagingTemplate,
                           @Value("${app.storage.upload-dir:./uploads}") String uploadDirectory) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("chat").normalize();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findContacts(Long userId, String roleName) {
        String contactRole = switch (roleName) {
            case "ROLE_PARENT", "ROLE_ADMIN" -> "ROLE_PARENT";
            case "ROLE_TEACHER" -> "ROLE_TEACHER";
            default -> "";
        };
        if (contactRole.isEmpty()) return List.of();
        return userRepository.findByRole_NameAndActiveTrueOrderByFullNameAsc(contactRole).stream()
                .filter(user -> !user.getId().equals(userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatConversationSummary> findConversations(Long userId, String search) {
        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<ChatConversationSummary> result = new ArrayList<>();
        for (ChatMember member : memberRepository.findActiveConversationsForUser(userId)) {
            ChatConversation conversation = member.getConversation();
            ChatMessage latest = messageRepository.findTopByConversationIdOrderByIdDesc(conversation.getId()).orElse(null);
            List<ChatMember> members = memberRepository.findByConversationIdOrderByJoinedAtAsc(conversation.getId());
            String displayTitle = resolveDisplayTitle(conversation, members, userId);
            if (!needle.isEmpty()) {
                boolean matches = displayTitle.toLowerCase(Locale.ROOT).contains(needle)
                        || members.stream().map(ChatMember::getUser).anyMatch(u -> u.getFullName().toLowerCase(Locale.ROOT).contains(needle))
                        || (latest != null && latest.getContent() != null && latest.getContent().toLowerCase(Locale.ROOT).contains(needle))
                        || messageRepository.existsByConversationIdAndContentContainingIgnoreCase(conversation.getId(), needle);
                if (!matches) continue;
            }
            long unread = member.isNotificationsEnabled()
                    ? messageRepository.countByConversationIdAndIdGreaterThanAndSenderIdNot(
                    conversation.getId(), member.getLastReadMessageId(), userId) : 0;
            result.add(new ChatConversationSummary(conversation, latest, unread, displayTitle, initials(displayTitle)));
        }
        return result;
    }

    @Override
    @Transactional
    public ChatConversation createConversation(Long creatorId, String creatorRole, List<Long> recipientIds, String title) {
        User creator = activeUser(creatorId);
        if (!Set.of("ROLE_PARENT", "ROLE_TEACHER", "ROLE_ADMIN").contains(creatorRole))
            throw new IllegalArgumentException("Tài khoản này không được phép tạo hội thoại.");
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        if (recipientIds != null) recipientIds.stream().filter(Objects::nonNull).filter(id -> !id.equals(creatorId)).forEach(uniqueIds::add);
        if (uniqueIds.size() != 1) throw new IllegalArgumentException("Hãy chọn đúng một người để bắt đầu tin nhắn riêng.");

        List<User> recipients = userRepository.findAllById(uniqueIds);
        if (recipients.size() != uniqueIds.size() || recipients.stream().anyMatch(u -> !Boolean.TRUE.equals(u.getActive()))) {
            throw new IllegalArgumentException("Một hoặc nhiều tài khoản được chọn không còn hoạt động.");
        }
        User recipient = recipients.getFirst();
        String recipientRole = recipient.getRole() == null ? "" : recipient.getRole().getName();
        String conversationType;
        if ("ROLE_PARENT".equals(creatorRole) && "ROLE_ADMIN".equals(recipientRole)) {
            User primaryAdmin = userRepository.findByUsername("admin")
                    .orElseThrow(() -> new IllegalArgumentException("Chưa cấu hình tài khoản quản trị viên chính."));
            if (!primaryAdmin.getId().equals(recipient.getId()))
                throw new IllegalArgumentException("Phụ huynh chỉ có thể nhắn cho quản trị viên chính.");
            conversationType = "PARENT_ADMIN";
        } else if ("ROLE_ADMIN".equals(creatorRole) && "ROLE_PARENT".equals(recipientRole)) {
            conversationType = "PARENT_ADMIN";
        } else if ("ROLE_TEACHER".equals(creatorRole) && "ROLE_TEACHER".equals(recipientRole)) {
            conversationType = "TEACHER";
        } else {
            throw new IllegalArgumentException(switch (creatorRole) {
                case "ROLE_PARENT" -> "Phụ huynh chỉ có thể nhắn riêng cho quản trị viên chính hoặc người dạy qua mục lớp đang học.";
                case "ROLE_TEACHER" -> "Người dạy hãy dùng hội thoại phụ huynh hoặc quản trị viên trong hộp thư.";
                default -> "Quản trị viên chỉ có thể tạo tin nhắn riêng với phụ huynh tại đây.";
            });
        }

        String directKey = directKey(conversationType, creatorId, recipient.getId());
        Optional<ChatConversation> existing = conversationRepository.findByDirectKey(directKey);
        if (existing.isEmpty() && "PARENT_ADMIN".equals(conversationType)) {
            // Reuse direct threads created before parent-to-admin messaging was enabled.
            existing = conversationRepository.findByDirectKey(directKey("ADMIN", creatorId, recipient.getId()));
        }
        if (existing.isPresent()) {
            ChatConversation found = existing.get();
            memberRepository.findByConversationIdAndUserId(found.getId(), creatorId).ifPresent(m -> m.setArchived(false));
            memberRepository.findByConversationIdAndUserId(found.getId(), recipient.getId()).ifPresent(m -> m.setArchived(false));
            memberRepository.flush();
            return found;
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setConversationType(conversationType);
        conversation.setDirectKey(directKey);
        conversation.setCreatedBy(creator);
        conversation.setTitle(recipient.getFullName());
        conversation = conversationRepository.save(conversation);
        addMember(conversation, creator);
        addMember(conversation, recipient);
        return conversation;
    }

    @Override
    @Transactional
    public ChatPage openConversation(Long conversationId, Long userId) {
        ChatConversation conversation = getConversation(conversationId);
        ChatMember current = requireMember(conversationId, userId);
        current.setArchived(false);
        messageRepository.findTopByConversationIdOrderByIdDesc(conversationId).ifPresent(message -> {
            if (message.getId() > current.getLastReadMessageId()) current.setLastReadMessageId(message.getId());
        });
        memberRepository.save(current);
        List<ChatMember> members = memberRepository.findByConversationIdOrderByJoinedAtAsc(conversationId);
        return new ChatPage(conversation, current, members, messageRepository.findByConversationIdOrderByIdAsc(conversationId),
                resolveDisplayTitle(conversation, members, userId));
    }

    @Override
    @Transactional
    public ChatMessage send(Long conversationId, Long senderId, String content, Long replyToId, MultipartFile file,
                            List<Long> mentionedUserIds, boolean important) {
        ChatConversation conversation = getConversation(conversationId);
        requireMember(conversationId, senderId);
        String cleanContent = content == null ? "" : content.trim();
        boolean hasFile = file != null && !file.isEmpty();
        if (cleanContent.isEmpty() && !hasFile) throw new IllegalArgumentException("Nhập tin nhắn hoặc đính kèm tệp trước khi gửi.");
        if (cleanContent.length() > 2000) throw new IllegalArgumentException("Tin nhắn không được vượt quá 2.000 ký tự.");

        LinkedHashSet<Long> mentionIds = new LinkedHashSet<>();
        if (mentionedUserIds != null) mentionedUserIds.stream().filter(Objects::nonNull).forEach(mentionIds::add);
        if (mentionIds.size() > 30) throw new IllegalArgumentException("Mỗi tin nhắn chỉ được nhắc tối đa 30 người.");
        User sender = activeUser(senderId);
        String senderRole = sender.getRole() == null ? "" : sender.getRole().getName();
        if (important && !Set.of("ROLE_TEACHER", "ROLE_ADMIN").contains(senderRole))
            throw new IllegalArgumentException("Chỉ giáo viên hoặc quản trị viên được đánh dấu tin quan trọng.");
        List<ChatMember> conversationMembers = memberRepository.findByConversationIdOrderByJoinedAtAsc(conversationId);
        Set<Long> memberIds = conversationMembers.stream().map(m -> m.getUser().getId()).collect(java.util.stream.Collectors.toSet());
        if (!memberIds.containsAll(mentionIds)) throw new IllegalArgumentException("Chỉ có thể nhắc thành viên của hội thoại.");

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setImportant(important);
        if (!mentionIds.isEmpty()) message.setMentionedUsers(new LinkedHashSet<>(userRepository.findAllById(mentionIds)));
        message.setContent(cleanContent.isEmpty() ? null : cleanContent);
        if (replyToId != null) {
            message.setReplyTo(messageRepository.findByIdAndConversationId(replyToId, conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Tin nhắn được trả lời không tồn tại.")));
        }
        if (hasFile) storeAttachment(file, message);
        ChatMessage saved = messageRepository.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        ChatMember member = requireMember(conversationId, senderId);
        member.setLastReadMessageId(saved.getId());
        memberRepository.save(member);
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, payload(saved));
        String category = conversation.getClassEntity() != null || conversationMembers.size() > 2 ? "MESSAGE_GROUP" : "MESSAGE_PRIVATE";
        String preview = cleanContent.isBlank() ? "Đã gửi tệp đính kèm." : cleanContent.substring(0, Math.min(140, cleanContent.length()));
        for (ChatMember recipient : conversationMembers) {
            Long recipientId = recipient.getUser().getId();
            if (recipientId.equals(senderId) || !recipient.isNotificationsEnabled()) continue;
            String title = mentionIds.contains(recipientId) ? sender.getFullName() + " đã nhắc đến bạn"
                    : (important ? "Tin nhắn quan trọng từ " : "Tin nhắn mới từ ") + sender.getFullName();
            notificationService.notifyUser(recipientId, "chat-message-" + saved.getId(), category, title, preview, "/chat/" + conversationId);
        }
        return saved;
    }

    @Override
    @Transactional
    public List<ChatMessagePayload> findMessagesAfter(Long conversationId, Long userId, Long afterId) {
        requireMember(conversationId, userId);
        List<ChatMessage> newMessages = messageRepository.findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, afterId == null ? 0 : afterId);
        if (!newMessages.isEmpty()) {
            ChatMember member = requireMember(conversationId, userId);
            member.setLastReadMessageId(newMessages.get(newMessages.size() - 1).getId());
            memberRepository.save(member);
        }
        return newMessages.stream().map(this::payload).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatMessage findMessageForMember(Long conversationId, Long messageId, Long userId) {
        requireMember(conversationId, userId);
        return messageRepository.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin nhắn."));
    }

    @Override
    @Transactional(readOnly = true)
    public Path resolveFile(ChatMessage message) {
        if (message.getStoredFilename() == null) throw new IllegalArgumentException("Tin nhắn này không có tệp đính kèm.");
        Path file = uploadDirectory.resolve(message.getStoredFilename()).normalize();
        if (!file.startsWith(uploadDirectory) || !Files.isRegularFile(file)) throw new IllegalArgumentException("Tệp đính kèm không còn tồn tại.");
        return file;
    }

    @Override
    @Transactional
    public void setAvatar(Long conversationId, Long userId, MultipartFile file) {
        ChatConversation conversation = getConversation(conversationId);
        requireMember(conversationId, userId);
        requireEditableGroup(conversation);
        requireManager(conversation, userId);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Chọn ảnh đại diện nhóm.");
        if (file.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("Ảnh nhóm không được vượt quá 5 MB.");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1);
        String extension = extension(original);
        if (!Set.of("jpg", "jpeg", "png", "gif", "webp").contains(extension)) throw new IllegalArgumentException("Ảnh nhóm cần là JPG, PNG, GIF hoặc WEBP.");
        String stored = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadDirectory);
            try (var input = file.getInputStream()) { Files.copy(input, uploadDirectory.resolve(stored), StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể lưu ảnh nhóm.", e);
        }
        String oldFilename = conversation.getAvatarFilename();
        conversation.setAvatarFilename(stored);
        conversationRepository.save(conversation);
        deleteStoredFile(oldFilename);
    }

    @Override
    @Transactional(readOnly = true)
    public Path resolveAvatar(Long conversationId, Long userId) {
        ChatConversation conversation = getConversation(conversationId);
        requireMember(conversationId, userId);
        if (conversation.getAvatarFilename() == null) throw new IllegalArgumentException("Hội thoại chưa có ảnh đại diện.");
        Path path = uploadDirectory.resolve(conversation.getAvatarFilename()).normalize();
        if (!path.startsWith(uploadDirectory) || !Files.isRegularFile(path)) throw new IllegalArgumentException("Ảnh nhóm không còn tồn tại.");
        return path;
    }

    @Override
    @Transactional
    public ChatMessageReaction toggleReaction(Long conversationId, Long messageId, Long userId, String emoji) {
        requireMember(conversationId, userId);
        if (!ALLOWED_REACTIONS.contains(emoji)) throw new IllegalArgumentException("Biểu cảm không hợp lệ.");
        ChatMessage message = messageRepository.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin nhắn."));
        Optional<ChatMessageReaction> existing = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            return null;
        }
        ChatMessageReaction reaction = new ChatMessageReaction();
        reaction.setMessage(message);
        reaction.setUser(activeUser(userId));
        reaction.setEmoji(emoji);
        return reactionRepository.save(reaction);
    }

    @Override
    @Transactional
    public void deleteForAll(Long conversationId, Long messageId, Long userId) {
        ChatMessage message = findMessageForMember(conversationId, messageId, userId);
        if (!Objects.equals(message.getSender().getId(), userId)) throw new IllegalArgumentException("Bạn chỉ có thể thu hồi tin nhắn do mình gửi.");
        deleteStoredFile(message.getStoredFilename());
        message.setStoredFilename(null);
        message.setOriginalFilename(null);
        message.setContent(null);
        message.setDeletedForAll(true);
        messageRepository.save(message);
    }

    @Override
    @Transactional
    public ChatMessage forward(Long sourceConversationId, Long messageId, Long targetConversationId, Long userId) {
        ChatMessage source = findMessageForMember(sourceConversationId, messageId, userId);
        requireMember(targetConversationId, userId);
        if (source.isDeletedForAll()) throw new IllegalArgumentException("Không thể chuyển tiếp tin nhắn đã thu hồi.");
        ChatMessage forwarded = new ChatMessage();
        forwarded.setConversation(getConversation(targetConversationId));
        forwarded.setSender(activeUser(userId));
        forwarded.setContent(source.getContent());
        forwarded.setForwardedFrom(source);
        ChatMessage saved = messageRepository.save(forwarded);
        ChatConversation target = getConversation(targetConversationId);
        target.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(target);
        return saved;
    }

    @Override
    @Transactional
    public void rename(Long conversationId, Long userId, String title) {
        ChatConversation conversation = getConversation(conversationId);
        requireMember(conversationId, userId);
        requireEditableGroup(conversation);
        requireManager(conversation, userId);
        if (title == null || title.isBlank() || title.trim().length() > 150) throw new IllegalArgumentException("Tên hội thoại từ 1 đến 150 ký tự.");
        conversation.setTitle(title.trim());
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public void addMembers(Long conversationId, Long actorId, List<Long> userIds) {
        ChatConversation conversation = getConversation(conversationId);
        requireMember(conversationId, actorId);
        requireEditableGroup(conversation);
        requireManager(conversation, actorId);
        if (userIds == null || userIds.isEmpty()) throw new IllegalArgumentException("Hãy chọn ít nhất một thành viên.");
        for (Long id : new LinkedHashSet<>(userIds)) {
            User user = activeUser(id);
            if (!"ADMIN".equals(conversation.getConversationType())
                    && (user.getRole() == null || !roleMatchesConversation(user.getRole().getName(), conversation.getConversationType()))) {
                throw new IllegalArgumentException("Nhóm phụ huynh/người dạy chỉ nhận thành viên cùng vai trò.");
            }
            if (!memberRepository.existsByConversationIdAndUserId(conversationId, id)) addMember(conversation, user);
        }
    }

    @Override
    @Transactional
    public void removeMember(Long conversationId, Long actorId, Long memberId) {
        ChatConversation conversation = getConversation(conversationId);
        requireMember(conversationId, actorId);
        requireEditableGroup(conversation);
        requireManager(conversation, actorId);
        ChatMember member = memberRepository.findByConversationIdAndUserId(conversationId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("Thành viên không tồn tại trong hội thoại."));
        memberRepository.delete(member);
    }

    @Override
    @Transactional
    public void archive(Long conversationId, Long userId) {
        ChatMember member = requireMember(conversationId, userId);
        member.setArchived(true);
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public void setNotifications(Long conversationId, Long userId, boolean enabled) {
        ChatMember member = requireMember(conversationId, userId);
        member.setNotificationsEnabled(enabled);
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public ChatConversation sendAdminMessageToParent(Long adminUserId, Long parentUserId, String content) {
        User parent = activeUser(parentUserId);
        if (parent.getRole() == null || !"ROLE_PARENT".equals(parent.getRole().getName())) throw new IllegalArgumentException("Tài khoản này không phải phụ huynh.");
        ChatConversation conversation = createConversation(adminUserId, "ROLE_ADMIN", List.of(parentUserId), null);
        send(conversation.getId(), adminUserId, content, null, null, List.of(), false);
        return conversation;
    }

    private ChatMessagePayload payload(ChatMessage message) {
        return new ChatMessagePayload(message.getId(), message.getSender().getId(), message.getSender().getFullName(),
                message.isDeletedForAll() ? null : message.getContent(), message.getOriginalFilename(),
                message.getStoredFilename() == null || message.isDeletedForAll() ? null
                        : "/chat/" + message.getConversation().getId() + "/files/" + message.getId(),
                message.getContentType(), message.getReplyTo() == null ? null : message.getReplyTo().getContent(),
                message.isDeletedForAll(), message.getCreatedAt(), message.isImportant(),
                message.getMentionedUsers().stream().map(User::getId).toList());
    }

    private void storeAttachment(MultipartFile file, ChatMessage message) {
        if (file.getSize() > MAX_ATTACHMENT_BYTES) throw new IllegalArgumentException("Tệp đính kèm không được vượt quá 20 MB.");
        String original = file.getOriginalFilename() == null ? "tep" : file.getOriginalFilename().replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1).trim();
        if (original.isBlank() || original.length() > 255) throw new IllegalArgumentException("Tên tệp đính kèm không hợp lệ.");
        String extension = extension(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) throw new IllegalArgumentException("Định dạng tệp chưa được hỗ trợ.");
        String storedName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadDirectory);
            try (var input = file.getInputStream()) {
                Files.copy(input, uploadDirectory.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể lưu tệp đính kèm.", e);
        }
        message.setStoredFilename(storedName);
        message.setOriginalFilename(original);
        message.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        message.setFileSize(file.getSize());
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private ChatConversation getConversation(Long id) {
        return conversationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hội thoại."));
    }

    private ChatMember requireMember(Long conversationId, Long userId) {
        return memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Bạn không có quyền truy cập hội thoại này."));
    }

    private User activeUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (!Boolean.TRUE.equals(user.getActive())) throw new IllegalArgumentException("Tài khoản đã bị khóa.");
        return user;
    }

    private void addMember(ChatConversation conversation, User user) {
        ChatMember member = new ChatMember();
        member.setConversation(conversation);
        member.setUser(user);
        memberRepository.save(member);
    }

    private void requireManager(ChatConversation conversation, Long actorId) {
        User actor = activeUser(actorId);
        boolean isAdmin = actor.getRole() != null && "ROLE_ADMIN".equals(actor.getRole().getName());
        if (!isAdmin && !Objects.equals(conversation.getCreatedBy().getId(), actorId)) {
            throw new IllegalArgumentException("Chỉ người tạo nhóm hoặc quản trị viên mới có thể thay đổi nhóm.");
        }
    }

    private void requireEditableGroup(ChatConversation conversation) {
        if (conversation.getDirectKey() != null || conversation.getClassEntity() != null
                || "CLASS".equals(conversation.getConversationType())) {
            throw new IllegalArgumentException("Hội thoại riêng và nhóm lớp được quản lý tự động, không thể thay đổi thành viên hoặc tên nhóm.");
        }
    }

    private boolean roleMatchesConversation(String role, String conversationType) {
        return ("PARENT".equals(conversationType) && "ROLE_PARENT".equals(role))
                || ("TEACHER".equals(conversationType) && "ROLE_TEACHER".equals(role));
    }

    private String directKey(String type, Long first, Long second) {
        long low = Math.min(first, second);
        long high = Math.max(first, second);
        return type + ":" + low + ":" + high;
    }

    private String initials(String title) {
        if (title == null || title.isBlank()) return "HC";
        String[] parts = title.trim().split("\\s+");
        return parts.length == 1 ? parts[0].substring(0, 1).toUpperCase(Locale.ROOT)
                : (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String resolveDisplayTitle(ChatConversation conversation, List<ChatMember> members, Long userId) {
        if (conversation.getDirectKey() != null) {
            return members.stream().map(ChatMember::getUser).filter(user -> !user.getId().equals(userId))
                    .map(User::getFullName).findFirst().orElse(conversation.getTitle());
        }
        return conversation.getTitle() == null || conversation.getTitle().isBlank() ? "Hội thoại" : conversation.getTitle();
    }

    private void deleteStoredFile(String filename) {
        if (filename == null) return;
        Path path = uploadDirectory.resolve(filename).normalize();
        if (!path.startsWith(uploadDirectory)) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }
}
