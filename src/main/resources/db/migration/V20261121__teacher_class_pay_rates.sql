CREATE TABLE IF NOT EXISTS `teacher_class_pay_rates` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `rate` DECIMAL(12,2) NOT NULL,
    `effective_from` DATE NOT NULL,
    `created_by` BIGINT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_teacher_class_pay_rate_effective` (`class_id`, `effective_from`),
    KEY `idx_teacher_class_pay_rates_class_date` (`class_id`, `effective_from`),
    CONSTRAINT `fk_teacher_class_pay_rates_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_teacher_class_pay_rates_admin` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
