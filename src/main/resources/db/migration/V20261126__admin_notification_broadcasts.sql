CREATE TABLE IF NOT EXISTS `notification_broadcasts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `created_by` BIGINT NOT NULL,
    `target_role` VARCHAR(30) NULL,
    `title` VARCHAR(180) NOT NULL,
    `body` VARCHAR(500) NOT NULL,
    `target_path` VARCHAR(2048) NOT NULL,
    `scheduled_at` DATETIME NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `attempt_count` INT NOT NULL DEFAULT 0,
    `last_error` VARCHAR(500) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `sent_at` DATETIME NULL,
    KEY `idx_notification_broadcast_due` (`status`, `scheduled_at`),
    CONSTRAINT `fk_notification_broadcast_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
