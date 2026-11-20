CREATE TABLE IF NOT EXISTS `push_subscriptions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `endpoint` TEXT NOT NULL,
    `endpoint_hash` CHAR(64) NOT NULL,
    `p256dh_key` VARCHAR(256) NOT NULL,
    `auth_key` VARCHAR(128) NOT NULL,
    `device_name` VARCHAR(100) NOT NULL DEFAULT 'Thiết bị của tôi',
    `user_agent` VARCHAR(512) NULL,
    `platform` VARCHAR(80) NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_seen_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_push_endpoint_hash` (`endpoint_hash`),
    KEY `idx_push_subscriptions_user_active` (`user_id`, `active`),
    CONSTRAINT `fk_push_subscriptions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notification_preferences` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `push_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `private_messages` TINYINT(1) NOT NULL DEFAULT 1,
    `group_messages` TINYINT(1) NOT NULL DEFAULT 1,
    `tuition` TINYINT(1) NOT NULL DEFAULT 1,
    `tuition_reminders` TINYINT(1) NOT NULL DEFAULT 1,
    `schedule_reminders` TINYINT(1) NOT NULL DEFAULT 1,
    `schedule_changes` TINYINT(1) NOT NULL DEFAULT 1,
    `attendance` TINYINT(1) NOT NULL DEFAULT 1,
    `learning_reports` TINYINT(1) NOT NULL DEFAULT 1,
    `homework` TINYINT(1) NOT NULL DEFAULT 1,
    `general` TINYINT(1) NOT NULL DEFAULT 1,
    `sound_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `preview_enabled` TINYINT(1) NOT NULL DEFAULT 0,
    `quiet_hours_enabled` TINYINT(1) NOT NULL DEFAULT 0,
    `quiet_start` TIME NULL,
    `quiet_end` TIME NULL,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_notification_preferences_user` (`user_id`),
    CONSTRAINT `fk_notification_preferences_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `event_key` VARCHAR(191) NOT NULL,
    `category` VARCHAR(40) NOT NULL,
    `title` VARCHAR(180) NOT NULL,
    `body` VARCHAR(500) NOT NULL,
    `target_path` VARCHAR(2048) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `read_at` TIMESTAMP NULL,
    `hidden_at` TIMESTAMP NULL,
    UNIQUE KEY `uk_user_notifications_event` (`user_id`, `event_key`),
    KEY `idx_user_notifications_unread` (`user_id`, `read_at`, `hidden_at`, `created_at`),
    KEY `idx_user_notifications_category` (`user_id`, `category`, `created_at`),
    CONSTRAINT `fk_user_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notification_outbox` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `notification_id` BIGINT NULL,
    `event_key` VARCHAR(191) NOT NULL,
    `category` VARCHAR(40) NOT NULL,
    `title` VARCHAR(180) NOT NULL,
    `body` VARCHAR(500) NOT NULL,
    `target_path` VARCHAR(2048) NULL,
    `scheduled_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `attempt_count` INT NOT NULL DEFAULT 0,
    `next_attempt_at` TIMESTAMP NULL,
    `last_error` VARCHAR(500) NULL,
    `accepted_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_notification_outbox_event` (`event_key`),
    KEY `idx_notification_outbox_pending` (`status`, `scheduled_at`, `next_attempt_at`),
    CONSTRAINT `fk_notification_outbox_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notification_outbox_notification` FOREIGN KEY (`notification_id`) REFERENCES `user_notifications` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notification_deliveries` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `outbox_id` BIGINT NOT NULL,
    `subscription_id` BIGINT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `attempt_count` INT NOT NULL DEFAULT 0,
    `accepted_at` TIMESTAMP NULL,
    `last_error` VARCHAR(500) NULL,
    UNIQUE KEY `uk_notification_delivery_device` (`outbox_id`, `subscription_id`),
    KEY `idx_notification_delivery_status` (`status`),
    CONSTRAINT `fk_notification_deliveries_outbox` FOREIGN KEY (`outbox_id`) REFERENCES `notification_outbox` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notification_deliveries_subscription` FOREIGN KEY (`subscription_id`) REFERENCES `push_subscriptions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `homework_assignments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `created_by` BIGINT NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `instructions` TEXT NOT NULL,
    `due_at` DATETIME NOT NULL,
    `attachment_stored_filename` VARCHAR(100) NULL,
    `attachment_original_filename` VARCHAR(255) NULL,
    `attachment_content_type` VARCHAR(100) NULL,
    `attachment_file_size` BIGINT NULL,
    `published_at` DATETIME NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_homework_class_due` (`class_id`, `due_at`, `published_at`),
    CONSTRAINT `fk_homework_assignments_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_homework_assignments_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `homework_submissions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `assignment_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `submitted_by` BIGINT NOT NULL,
    `attachment_stored_filename` VARCHAR(100) NOT NULL,
    `attachment_original_filename` VARCHAR(255) NOT NULL,
    `attachment_content_type` VARCHAR(100) NULL,
    `attachment_file_size` BIGINT NOT NULL,
    `submitted_at` DATETIME NOT NULL,
    `late` TINYINT(1) NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    `score` DECIMAL(4,2) NULL,
    `feedback` VARCHAR(1000) NULL,
    `reviewed_by` BIGINT NULL,
    `reviewed_at` DATETIME NULL,
    UNIQUE KEY `uk_homework_assignment_student` (`assignment_id`, `student_id`),
    KEY `idx_homework_submissions_student` (`student_id`, `submitted_at`),
    CONSTRAINT `fk_homework_submissions_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `homework_assignments` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_homework_submissions_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_homework_submissions_submitter` FOREIGN KEY (`submitted_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_homework_submissions_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Attach class conversations to class rosters; existing one-to-one chats remain intact.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_conversations' AND column_name = 'class_id') = 0,
    'ALTER TABLE `chat_conversations` ADD COLUMN `class_id` BIGINT NULL', 'SELECT 1');
PREPARE pwa_stmt FROM @ddl; EXECUTE pwa_stmt; DEALLOCATE PREPARE pwa_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'chat_conversations' AND index_name = 'uk_chat_conversations_class') = 0,
    'ALTER TABLE `chat_conversations` ADD UNIQUE KEY `uk_chat_conversations_class` (`class_id`)', 'SELECT 1');
PREPARE pwa_stmt FROM @ddl; EXECUTE pwa_stmt; DEALLOCATE PREPARE pwa_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'chat_conversations' AND constraint_name = 'fk_chat_conversations_class') = 0,
    'ALTER TABLE `chat_conversations` ADD CONSTRAINT `fk_chat_conversations_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE', 'SELECT 1');
PREPARE pwa_stmt FROM @ddl; EXECUTE pwa_stmt; DEALLOCATE PREPARE pwa_stmt;

CREATE TABLE IF NOT EXISTS `chat_message_mentions` (
    `message_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    PRIMARY KEY (`message_id`, `user_id`),
    KEY `idx_chat_mentions_user` (`user_id`, `message_id`),
    CONSTRAINT `fk_chat_mentions_message` FOREIGN KEY (`message_id`) REFERENCES `chat_messages` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_mentions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
