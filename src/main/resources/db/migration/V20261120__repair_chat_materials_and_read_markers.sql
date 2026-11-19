-- Recreate feature tables if an installation recorded the earlier migration but is missing its tables.
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
    CONSTRAINT `fk_learning_materials_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_learning_material_source` CHECK (`stored_filename` IS NOT NULL OR `external_url` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `parent_teacher_conversations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `parent_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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

ALTER TABLE `parent_teacher_conversations`
    ADD COLUMN `parent_last_read_message_id` BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN `teacher_last_read_message_id` BIGINT NOT NULL DEFAULT 0;

ALTER TABLE `teacher_admin_conversations`
    ADD COLUMN `teacher_last_read_message_id` BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN `admin_last_read_message_id` BIGINT NOT NULL DEFAULT 0;
