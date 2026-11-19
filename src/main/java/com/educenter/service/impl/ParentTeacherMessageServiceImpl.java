package com.educenter.service.impl;

import com.educenter.dto.ConversationPage;
import com.educenter.dto.ConversationTarget;
import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.ParentTeacherMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ParentTeacherMessageServiceImpl implements ParentTeacherMessageService {

    private final ParentTeacherConversationRepository conversationRepository;
    private final ParentTeacherMessageRepository messageRepository;
    private final ParentRepository parentRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final UserRepository userRepository;

    public ParentTeacherMessageServiceImpl(ParentTeacherConversationRepository conversationRepository,
                                          ParentTeacherMessageRepository messageRepository,
                                          ParentRepository parentRepository,
                                          ClassRepository classRepository,
                                          ClassStudentRepository classStudentRepository,
                                          UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.parentRepository = parentRepository;
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ClassEntity> findParentClasses(Long parentUserId) {
        Map<Long, ClassEntity> classes = new LinkedHashMap<>();
        for (ClassStudent enrollment : classStudentRepository.findByStudentParentUserId(parentUserId)) {
            if ("ACTIVE".equals(enrollment.getStatus())) {
                classes.putIfAbsent(enrollment.getClassEntity().getId(), enrollment.getClassEntity());
            }
        }
        return new ArrayList<>(classes.values());
    }

    @Override
    public List<ConversationTarget> findTeacherTargets(Long teacherUserId) {
        Map<TargetKey, TargetAccumulator> grouped = new LinkedHashMap<>();
        for (ClassEntity classEntity : classRepository.findByTeacherUserId(teacherUserId)) {
            for (ClassStudent enrollment : classStudentRepository.findByClassEntityIdAndStatus(classEntity.getId(), "ACTIVE")) {
                Parent parent = enrollment.getStudent().getParent();
                TargetKey key = new TargetKey(classEntity.getId(), parent.getId());
                TargetAccumulator target = grouped.computeIfAbsent(key, ignored -> new TargetAccumulator(classEntity, parent));
                target.studentNames.add(enrollment.getStudent().getFullName());
            }
        }
        return grouped.values().stream()
                .map(target -> new ConversationTarget(target.classEntity, target.parent, List.copyOf(target.studentNames)))
                .toList();
    }

    @Override
    @Transactional
    public ConversationPage openForParent(Long parentUserId, Long classId) {
        Parent parent = parentRepository.findByUserId(parentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ phụ huynh."));
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học."));
        requireParentEnrolled(parentUserId, classId);
        ParentTeacherConversation conversation = getOrCreateConversation(parent, classEntity);
        markParentMessagesRead(conversation);
        return page(conversation);
    }

    @Override
    @Transactional
    public ConversationPage openForTeacher(Long teacherUserId, Long classId, Long parentId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học."));
        requireTeacherOwns(classEntity, teacherUserId);
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phụ huynh."));
        requireParentHasActiveStudent(parentId, classId);
        ParentTeacherConversation conversation = getOrCreateConversation(parent, classEntity);
        markTeacherMessagesRead(conversation);
        return page(conversation);
    }

    @Override
    @Transactional
    public void sendAsParent(Long parentUserId, Long classId, String content) {
        ParentTeacherConversation conversation = openForParent(parentUserId, classId).conversation();
        saveMessage(conversation, parentUserId, content);
    }

    @Override
    @Transactional
    public void sendAsTeacher(Long teacherUserId, Long classId, Long parentId, String content) {
        ParentTeacherConversation conversation = openForTeacher(teacherUserId, classId, parentId).conversation();
        saveMessage(conversation, teacherUserId, content);
    }

    private ParentTeacherConversation getOrCreateConversation(Parent parent, ClassEntity classEntity) {
        return conversationRepository.findByClassEntityIdAndParentId(classEntity.getId(), parent.getId())
                .orElseGet(() -> {
                    ParentTeacherConversation created = new ParentTeacherConversation();
                    created.setClassEntity(classEntity);
                    created.setParent(parent);
                    return conversationRepository.save(created);
                });
    }

    private ConversationPage page(ParentTeacherConversation conversation) {
        return new ConversationPage(conversation,
                messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(conversation.getId()));
    }

    private void markParentMessagesRead(ParentTeacherConversation conversation) {
        messageRepository.findTopByConversationIdOrderByIdDesc(conversation.getId()).ifPresent(message -> {
            if (message.getId() > conversation.getParentLastReadMessageId()) {
                conversation.setParentLastReadMessageId(message.getId());
                conversationRepository.save(conversation);
            }
        });
    }

    private void markTeacherMessagesRead(ParentTeacherConversation conversation) {
        messageRepository.findTopByConversationIdOrderByIdDesc(conversation.getId()).ifPresent(message -> {
            if (message.getId() > conversation.getTeacherLastReadMessageId()) {
                conversation.setTeacherLastReadMessageId(message.getId());
                conversationRepository.save(conversation);
            }
        });
    }

    private void saveMessage(ParentTeacherConversation conversation, Long senderId, String content) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Nội dung tin nhắn không được để trống.");
        if (content.trim().length() > 2000) throw new IllegalArgumentException("Tin nhắn không được vượt quá 2.000 ký tự.");
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản gửi tin nhắn."));
        ParentTeacherMessage message = new ParentTeacherMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());
        messageRepository.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private void requireParentEnrolled(Long parentUserId, Long classId) {
        boolean allowed = classStudentRepository.findByStudentParentUserId(parentUserId).stream()
                .anyMatch(e -> "ACTIVE".equals(e.getStatus()) && Objects.equals(e.getClassEntity().getId(), classId));
        if (!allowed) throw new IllegalArgumentException("Bạn không có học sinh đang theo học lớp này.");
    }

    private void requireParentHasActiveStudent(Long parentId, Long classId) {
        boolean allowed = classStudentRepository.findByClassEntityIdAndStatus(classId, "ACTIVE").stream()
                .anyMatch(e -> Objects.equals(e.getStudent().getParent().getId(), parentId));
        if (!allowed) throw new IllegalArgumentException("Phụ huynh không có học sinh đang theo học lớp này.");
    }

    private void requireTeacherOwns(ClassEntity classEntity, Long teacherUserId) {
        if (classEntity.getTeacher() == null || classEntity.getTeacher().getUser() == null
                || !Objects.equals(classEntity.getTeacher().getUser().getId(), teacherUserId)) {
            throw new IllegalArgumentException("Bạn không phụ trách lớp học này.");
        }
    }

    private record TargetKey(Long classId, Long parentId) {}

    private static final class TargetAccumulator {
        private final ClassEntity classEntity;
        private final Parent parent;
        private final List<String> studentNames = new ArrayList<>();

        private TargetAccumulator(ClassEntity classEntity, Parent parent) {
            this.classEntity = classEntity;
            this.parent = parent;
        }
    }
}
