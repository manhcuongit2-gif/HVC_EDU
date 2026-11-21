ALTER TABLE `exam_assignments`
    MODIFY COLUMN `blueprint_id` BIGINT NULL,
    ADD COLUMN `exam_type` VARCHAR(16) NOT NULL DEFAULT 'ONLINE' AFTER `title`,
    ADD COLUMN `pdf_stored_filename` VARCHAR(80) NULL AFTER `exam_type`,
    ADD COLUMN `pdf_original_filename` VARCHAR(255) NULL AFTER `pdf_stored_filename`,
    ADD COLUMN `pdf_content_type` VARCHAR(100) NULL AFTER `pdf_original_filename`,
    ADD COLUMN `pdf_file_size` BIGINT NULL AFTER `pdf_content_type`;

CREATE TABLE `exam_pdf_submissions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `assignment_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `version_number` INT NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `score` DECIMAL(5,2) NULL,
    `feedback` VARCHAR(1000) NULL,
    `submitted_at` DATETIME NOT NULL,
    `reviewed_by` BIGINT NULL,
    `reviewed_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_pdf_submission_version` (`assignment_id`, `student_id`, `version_number`),
    KEY `idx_exam_pdf_submission_student` (`student_id`, `assignment_id`, `submitted_at`),
    CONSTRAINT `fk_exam_pdf_submission_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `exam_assignments` (`id`),
    CONSTRAINT `fk_exam_pdf_submission_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
    CONSTRAINT `fk_exam_pdf_submission_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `exam_pdf_submission_files` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `submission_id` BIGINT NOT NULL,
    `stored_filename` VARCHAR(80) NOT NULL,
    `original_filename` VARCHAR(255) NOT NULL,
    `content_type` VARCHAR(100) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `position` INT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_exam_pdf_submission_files_submission` (`submission_id`, `position`),
    CONSTRAINT `fk_exam_pdf_submission_files_submission` FOREIGN KEY (`submission_id`) REFERENCES `exam_pdf_submissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
