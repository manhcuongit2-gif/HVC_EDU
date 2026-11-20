package com.educenter.service.impl;

import com.educenter.dto.TeacherAdminConversationPage;
import com.educenter.dto.LegacyMessagePayload;
import com.educenter.entity.Teacher;
import com.educenter.entity.TeacherAdminConversation;
import com.educenter.entity.TeacherAdminMessage;
import com.educenter.entity.User;
import com.educenter.repository.TeacherAdminConversationRepository;
import com.educenter.repository.TeacherAdminMessageRepository;
import com.educenter.repository.TeacherRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.TeacherAdminMessageService;
import com.educenter.service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class TeacherAdminMessageServiceImpl implements TeacherAdminMessageService {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String TEACHER_ROLE = "ROLE_TEACHER";

    private final TeacherAdminConversationRepository conversationRepository;
    private final TeacherAdminMessageRepository messageRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public TeacherAdminMessageServiceImpl(TeacherAdminConversationRepository conversationRepository,
                                          TeacherAdminMessageRepository messageRepository,
                                          TeacherRepository teacherRepository,
                                          UserRepository userRepository,
                                          NotificationService notificationService,
                                          SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public List<Teacher> findTeachersForAdmin(Long adminUserId) {
        requirePrimaryAdmin(adminUserId);
        return teacherRepository.findByUserActiveTrue().stream()
                .filter(teacher -> hasRole(teacher.getUser(), TEACHER_ROLE))
                .sorted(Comparator.comparing(teacher -> teacher.getUser().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional
    public TeacherAdminConversationPage openForTeacher(Long teacherUserId) {
        User teacher = requireTeacher(teacherUserId);
        User admin = findPrimaryAdmin();
        TeacherAdminConversation conversation = getOrCreate(teacher, admin);
        markTeacherMessagesRead(conversation);
        return page(conversation);
    }

    @Override
    @Transactional
    public TeacherAdminConversationPage openForAdmin(Long adminUserId, Long teacherUserId) {
        User admin = requirePrimaryAdmin(adminUserId);
        User teacher = requireTeacher(teacherUserId);
        TeacherAdminConversation conversation = getOrCreate(teacher, admin);
        markAdminMessagesRead(conversation);
        return page(conversation);
    }

    @Override
    @Transactional
    public void sendAsTeacher(Long teacherUserId, String content) {
        TeacherAdminConversation conversation = openForTeacher(teacherUserId).conversation();
        saveMessage(conversation, teacherUserId, content);
    }

    @Override
    @Transactional
    public void sendAsAdmin(Long adminUserId, Long teacherUserId, String content) {
        TeacherAdminConversation conversation = openForAdmin(adminUserId, teacherUserId).conversation();
        saveMessage(conversation, adminUserId, content);
    }

    @Override
    @Transactional
    public List<LegacyMessagePayload> findMessagesAfterForTeacher(Long teacherUserId, Long afterId) {
        TeacherAdminConversation conversation = getOrCreate(requireTeacher(teacherUserId), findPrimaryAdmin());
        List<TeacherAdminMessage> messages = messageRepository.findByConversationIdAndIdGreaterThanOrderByIdAsc(
                conversation.getId(), afterId == null ? 0L : Math.max(0L, afterId));
        messages.stream().map(TeacherAdminMessage::getId).max(Long::compareTo).ifPresent(conversation::setTeacherLastReadMessageId);
        return messages.stream().map(LegacyMessagePayload::from).toList();
    }

    @Override
    @Transactional
    public List<LegacyMessagePayload> findMessagesAfterForAdmin(Long adminUserId, Long teacherUserId, Long afterId) {
        TeacherAdminConversation conversation = getOrCreate(requireTeacher(teacherUserId), requirePrimaryAdmin(adminUserId));
        List<TeacherAdminMessage> messages = messageRepository.findByConversationIdAndIdGreaterThanOrderByIdAsc(
                conversation.getId(), afterId == null ? 0L : Math.max(0L, afterId));
        messages.stream().map(TeacherAdminMessage::getId).max(Long::compareTo).ifPresent(conversation::setAdminLastReadMessageId);
        return messages.stream().map(LegacyMessagePayload::from).toList();
    }

    private User requirePrimaryAdmin(Long adminUserId) {
        User admin = findPrimaryAdmin();
        if (!Objects.equals(admin.getId(), adminUserId)) {
            throw new IllegalArgumentException("Chỉ tài khoản admin chính được sử dụng hội thoại này.");
        }
        return admin;
    }

    private User findPrimaryAdmin() {
        User admin = userRepository.findByUsername(ADMIN_USERNAME)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản admin chính."));
        if (!Boolean.TRUE.equals(admin.getActive()) || !hasRole(admin, ADMIN_ROLE)) {
            throw new IllegalArgumentException("Tài khoản admin chính hiện không hoạt động.");
        }
        return admin;
    }

    private User requireTeacher(Long teacherUserId) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ người dạy."));
        User user = teacher.getUser();
        if (user == null || !Boolean.TRUE.equals(user.getActive()) || !hasRole(user, TEACHER_ROLE)) {
            throw new IllegalArgumentException("Tài khoản người dạy hiện không hoạt động.");
        }
        return user;
    }

    private boolean hasRole(User user, String expectedRole) {
        return user != null && user.getRole() != null && expectedRole.equals(user.getRole().getName());
    }

    private TeacherAdminConversation getOrCreate(User teacher, User admin) {
        return conversationRepository.findByTeacherUserIdAndAdminUserId(teacher.getId(), admin.getId())
                .orElseGet(() -> {
                    TeacherAdminConversation conversation = new TeacherAdminConversation();
                    conversation.setTeacherUser(teacher);
                    conversation.setAdminUser(admin);
                    return conversationRepository.save(conversation);
                });
    }

    private TeacherAdminConversationPage page(TeacherAdminConversation conversation) {
        return new TeacherAdminConversationPage(conversation,
                messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(conversation.getId()));
    }

    private void markTeacherMessagesRead(TeacherAdminConversation conversation) {
        messageRepository.findTopByConversationIdOrderByIdDesc(conversation.getId()).ifPresent(message -> {
            if (message.getId() > conversation.getTeacherLastReadMessageId()) {
                conversation.setTeacherLastReadMessageId(message.getId());
                conversationRepository.save(conversation);
            }
        });
    }

    private void markAdminMessagesRead(TeacherAdminConversation conversation) {
        messageRepository.findTopByConversationIdOrderByIdDesc(conversation.getId()).ifPresent(message -> {
            if (message.getId() > conversation.getAdminLastReadMessageId()) {
                conversation.setAdminLastReadMessageId(message.getId());
                conversationRepository.save(conversation);
            }
        });
    }

    private void saveMessage(TeacherAdminConversation conversation, Long senderId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống.");
        }
        if (content.trim().length() > 2000) {
            throw new IllegalArgumentException("Tin nhắn không được vượt quá 2.000 ký tự.");
        }
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản gửi tin nhắn."));
        TeacherAdminMessage message = new TeacherAdminMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());
        TeacherAdminMessage savedMessage = messageRepository.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        messagingTemplate.convertAndSend("/topic/teacher-admin/" + conversation.getId(), LegacyMessagePayload.from(savedMessage));
        Long recipientId = Objects.equals(senderId, conversation.getTeacherUser().getId())
                ? conversation.getAdminUser().getId() : conversation.getTeacherUser().getId();
        String targetPath = recipientId.equals(conversation.getAdminUser().getId())
                ? "/admin/messages/" + conversation.getTeacherUser().getId() : "/teacher/admin-messages";
        notificationService.notifyUser(recipientId, "teacher-admin-message-" + savedMessage.getId(), "MESSAGE_PRIVATE",
                "Tin nhắn mới từ " + savedMessage.getSender().getFullName(), content.trim().substring(0, Math.min(140, content.trim().length())), targetPath);
    }
}
