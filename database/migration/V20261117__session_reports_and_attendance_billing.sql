-- Apply once to an existing educenter_db. This migration is additive and does not drop data.
USE `educenter_db`;

ALTER TABLE `classes`
    ADD COLUMN `session_fee` DECIMAL(12,2) NULL AFTER `tuition_fee`;

ALTER TABLE `class_students`
    ADD COLUMN `dropped_date` DATE NULL AFTER `enrolled_date`;

ALTER TABLE `tuition`
    ADD COLUMN `billing_period` DATE NULL AFTER `description`,
    ADD COLUMN `billing_sequence` INT NULL AFTER `billing_period`,
    ADD COLUMN `auto_generated` TINYINT(1) NOT NULL DEFAULT 0 AFTER `billing_sequence`,
    ADD UNIQUE KEY `uk_tuition_period_sequence` (`student_id`, `class_id`, `billing_period`, `billing_sequence`);

-- Existing descriptions such as "Học phí Tháng 10/2026" provide the best period.
-- If the year is absent, use the due-date year; otherwise use the due-date month.
UPDATE `tuition`
SET `billing_period` = CASE
    WHEN `description` LIKE '%Tháng %/%' THEN
        STR_TO_DATE(CONCAT('01/', SUBSTRING_INDEX(SUBSTRING_INDEX(`description`, 'Tháng ', -1), ' ', 1)), '%d/%m/%Y')
    WHEN `description` LIKE '%Tháng %' AND `due_date` IS NOT NULL THEN
        STR_TO_DATE(CONCAT('01/', SUBSTRING_INDEX(SUBSTRING_INDEX(`description`, 'Tháng ', -1), ' ', 1), '/', YEAR(`due_date`)), '%d/%m/%Y')
    WHEN `due_date` IS NOT NULL THEN
        DATE_SUB(`due_date`, INTERVAL DAYOFMONTH(`due_date`) - 1 DAY)
    ELSE NULL
END
WHERE `billing_period` IS NULL;

UPDATE `class_students` SET `dropped_date` = CURRENT_DATE WHERE `status` = 'DROPPED' AND `dropped_date` IS NULL;

CREATE TABLE `class_sessions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `schedule_id` BIGINT NULL,
    `room_id` BIGINT NULL,
    `occurrence_date` DATE NOT NULL,
    `session_date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    `manually_adjusted` TINYINT(1) NOT NULL DEFAULT 0,
    `published_at` TIMESTAMP NULL,
    `published_by` BIGINT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_class_session_occurrence` (`schedule_id`, `occurrence_date`),
    KEY `idx_class_sessions_date` (`session_date`),
    CONSTRAINT `fk_class_sessions_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_class_sessions_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `schedules` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_class_sessions_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_class_sessions_publisher` FOREIGN KEY (`published_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `session_student_reports` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `attendance_status` VARCHAR(20) NOT NULL,
    `homework_score` DECIMAL(4,2) NULL,
    `comment` VARCHAR(1000) NOT NULL DEFAULT '',
    `good_evaluation_count` INT NOT NULL DEFAULT 0,
    `note` VARCHAR(500) NULL,
    `created_by` BIGINT NULL,
    `updated_by` BIGINT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_session_student_report` (`session_id`, `student_id`),
    CONSTRAINT `fk_session_reports_session` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_session_reports_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_session_reports_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_session_reports_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_session_homework_score` CHECK (`homework_score` IS NULL OR (`homework_score` >= 0 AND `homework_score` <= 10)),
    CONSTRAINT `chk_session_good_count` CHECK (`good_evaluation_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `monthly_student_reports` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `report_month` DATE NOT NULL,
    `present_count` INT NOT NULL DEFAULT 0,
    `absent_count` INT NOT NULL DEFAULT 0,
    `excused_count` INT NOT NULL DEFAULT 0,
    `homework_average` DECIMAL(4,2) NULL,
    `good_evaluation_count` INT NOT NULL DEFAULT 0,
    `teacher_comment` VARCHAR(1000) NOT NULL DEFAULT '',
    `published_at` TIMESTAMP NULL,
    `published_by` BIGINT NULL,
    `updated_by` BIGINT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_monthly_student_report` (`class_id`, `student_id`, `report_month`),
    CONSTRAINT `fk_monthly_reports_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_monthly_reports_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_monthly_reports_publisher` FOREIGN KEY (`published_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_monthly_reports_updater` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
