-- Repair older databases where a migration was recorded but the feature tables
-- were removed or never created. CREATE IF NOT EXISTS keeps this safe on healthy DBs.
CREATE TABLE IF NOT EXISTS `learning_materials` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `stored_filename` VARCHAR(100) NULL,
    `original_filename` VARCHAR(255) NULL,
    `content_type` VARCHAR(100) NULL,
    `file_size` BIGINT NULL,
    `external_url` VARCHAR(2048) NULL,
    `created_by` BIGINT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_learning_materials_class` (`class_id`, `created_at`),
    CONSTRAINT `fk_learning_materials_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_learning_materials_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `parent_teacher_conversations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `parent_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `parent_last_read_message_id` BIGINT NOT NULL DEFAULT 0,
    `teacher_last_read_message_id` BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_parent_teacher_conversation` (`class_id`, `parent_id`),
    CONSTRAINT `fk_conversations_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_conversations_parent` FOREIGN KEY (`parent_id`) REFERENCES `parents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `parent_teacher_messages` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` VARCHAR(2000) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_parent_teacher_messages_conversation` (`conversation_id`, `created_at`),
    CONSTRAINT `fk_parent_teacher_messages_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `parent_teacher_conversations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_parent_teacher_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `teacher_admin_conversations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `teacher_user_id` BIGINT NOT NULL,
    `admin_user_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `teacher_last_read_message_id` BIGINT NOT NULL DEFAULT 0,
    `admin_last_read_message_id` BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_teacher_admin_conversation` (`teacher_user_id`, `admin_user_id`),
    CONSTRAINT `fk_teacher_admin_conversations_teacher` FOREIGN KEY (`teacher_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_teacher_admin_conversations_admin` FOREIGN KEY (`admin_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `teacher_admin_messages` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` VARCHAR(2000) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_teacher_admin_messages_conversation` (`conversation_id`, `created_at`),
    CONSTRAINT `fk_teacher_admin_messages_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `teacher_admin_conversations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_teacher_admin_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Existing tables can be present without their read markers. Add missing columns safely.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'parent_teacher_conversations' AND column_name = 'parent_last_read_message_id') = 0,
    'ALTER TABLE `parent_teacher_conversations` ADD COLUMN `parent_last_read_message_id` BIGINT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE feature_stmt FROM @ddl; EXECUTE feature_stmt; DEALLOCATE PREPARE feature_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'parent_teacher_conversations' AND column_name = 'teacher_last_read_message_id') = 0,
    'ALTER TABLE `parent_teacher_conversations` ADD COLUMN `teacher_last_read_message_id` BIGINT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE feature_stmt FROM @ddl; EXECUTE feature_stmt; DEALLOCATE PREPARE feature_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_admin_conversations' AND column_name = 'teacher_last_read_message_id') = 0,
    'ALTER TABLE `teacher_admin_conversations` ADD COLUMN `teacher_last_read_message_id` BIGINT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE feature_stmt FROM @ddl; EXECUTE feature_stmt; DEALLOCATE PREPARE feature_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teacher_admin_conversations' AND column_name = 'admin_last_read_message_id') = 0,
    'ALTER TABLE `teacher_admin_conversations` ADD COLUMN `admin_last_read_message_id` BIGINT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE feature_stmt FROM @ddl; EXECUTE feature_stmt; DEALLOCATE PREPARE feature_stmt;

-- A membership-based conversation supports parent groups, teacher groups and
-- admin conversations with any active account while preserving the older inboxes.
CREATE TABLE IF NOT EXISTS `chat_conversations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(150) NULL,
    `conversation_type` VARCHAR(20) NOT NULL,
    `direct_key` VARCHAR(150) NULL,
    `avatar_filename` VARCHAR(100) NULL,
    `created_by` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_chat_direct_key` (`direct_key`),
    KEY `idx_chat_conversations_updated` (`updated_at`),
    CONSTRAINT `fk_chat_conversations_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_members` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `last_read_message_id` BIGINT NOT NULL DEFAULT 0,
    `archived` TINYINT(1) NOT NULL DEFAULT 0,
    `notifications_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_chat_member` (`conversation_id`, `user_id`),
    KEY `idx_chat_member_user` (`user_id`, `archived`),
    CONSTRAINT `fk_chat_members_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `chat_conversations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_messages` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` VARCHAR(2000) NULL,
    `stored_filename` VARCHAR(100) NULL,
    `original_filename` VARCHAR(255) NULL,
    `content_type` VARCHAR(100) NULL,
    `file_size` BIGINT NULL,
    `reply_to_message_id` BIGINT NULL,
    `forwarded_from_message_id` BIGINT NULL,
    `deleted_for_all` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_chat_messages_conversation` (`conversation_id`, `id`),
    CONSTRAINT `fk_chat_messages_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `chat_conversations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_messages_reply` FOREIGN KEY (`reply_to_message_id`) REFERENCES `chat_messages` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_chat_messages_forward` FOREIGN KEY (`forwarded_from_message_id`) REFERENCES `chat_messages` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_message_reactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `message_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `emoji` VARCHAR(16) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_chat_reaction` (`message_id`, `user_id`, `emoji`),
    CONSTRAINT `fk_chat_reactions_message` FOREIGN KEY (`message_id`) REFERENCES `chat_messages` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_reactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
