INSERT INTO `roles` (`name`, `description`)
SELECT 'ROLE_STUDENT', 'Học sinh'
WHERE NOT EXISTS (SELECT 1 FROM `roles` WHERE `name` = 'ROLE_STUDENT');

ALTER TABLE `students`
    ADD COLUMN `user_id` BIGINT NULL,
    ADD UNIQUE KEY `uk_students_user` (`user_id`),
    ADD CONSTRAINT `fk_students_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

ALTER TABLE `homework_submissions`
    ADD COLUMN `submission_text` TEXT NULL AFTER `submitted_by`,
    MODIFY COLUMN `attachment_stored_filename` VARCHAR(100) NULL,
    MODIFY COLUMN `attachment_original_filename` VARCHAR(255) NULL,
    MODIFY COLUMN `attachment_file_size` BIGINT NULL;

CREATE TABLE `push_subscription_users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `subscription_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_seen_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_push_subscription_user` (`subscription_id`, `user_id`),
    KEY `idx_push_subscription_users_user_active` (`user_id`, `active`),
    CONSTRAINT `fk_push_subscription_users_subscription` FOREIGN KEY (`subscription_id`) REFERENCES `push_subscriptions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_push_subscription_users_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `push_subscription_users` (`subscription_id`, `user_id`, `active`, `created_at`, `last_seen_at`)
SELECT `id`, `user_id`, `active`, `created_at`, `last_seen_at`
FROM `push_subscriptions`;
