CREATE TABLE `exam_questions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `owner_teacher_id` BIGINT NOT NULL,
    `subject_id` BIGINT NOT NULL,
    `grade_level` VARCHAR(20) NOT NULL,
    `topic` VARCHAR(150) NULL,
    `question_type` VARCHAR(24) NOT NULL,
    `difficulty` VARCHAR(20) NOT NULL,
    `prompt` TEXT NOT NULL,
    `options_json` LONGTEXT NULL,
    `answer_key_json` LONGTEXT NOT NULL,
    `explanation` TEXT NULL,
    `default_points` DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_exam_questions_owner_filter` (`owner_teacher_id`, `subject_id`, `grade_level`, `question_type`, `difficulty`, `active`),
    KEY `idx_exam_questions_topic` (`topic`),
    CONSTRAINT `fk_exam_questions_teacher` FOREIGN KEY (`owner_teacher_id`) REFERENCES `teachers` (`id`),
    CONSTRAINT `fk_exam_questions_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `exam_blueprints` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `owner_teacher_id` BIGINT NOT NULL,
    `subject_id` BIGINT NOT NULL,
    `grade_level` VARCHAR(20) NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `duration_minutes` INT NOT NULL,
    `total_points` DECIMAL(8,2) NOT NULL DEFAULT 10.00,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_exam_blueprints_owner` (`owner_teacher_id`, `created_at`),
    CONSTRAINT `fk_exam_blueprints_teacher` FOREIGN KEY (`owner_teacher_id`) REFERENCES `teachers` (`id`),
    CONSTRAINT `fk_exam_blueprints_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `exam_matrix_cells` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `blueprint_id` BIGINT NOT NULL,
    `topic` VARCHAR(150) NULL,
    `question_type` VARCHAR(24) NOT NULL,
    `difficulty` VARCHAR(20) NOT NULL,
    `question_count` INT NOT NULL,
    `cell_points` DECIMAL(8,2) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_exam_matrix_blueprint` (`blueprint_id`),
    CONSTRAINT `fk_exam_matrix_blueprint` FOREIGN KEY (`blueprint_id`) REFERENCES `exam_blueprints` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `exam_blueprint_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `blueprint_id` BIGINT NOT NULL,
    `matrix_cell_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `points` DECIMAL(8,2) NOT NULL,
    `position` INT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_blueprint_question` (`blueprint_id`, `question_id`),
    KEY `idx_exam_blueprint_items_position` (`blueprint_id`, `position`),
    CONSTRAINT `fk_exam_blueprint_items_blueprint` FOREIGN KEY (`blueprint_id`) REFERENCES `exam_blueprints` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_exam_blueprint_items_cell` FOREIGN KEY (`matrix_cell_id`) REFERENCES `exam_matrix_cells` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_exam_blueprint_items_question` FOREIGN KEY (`question_id`) REFERENCES `exam_questions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `exam_assignments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `blueprint_id` BIGINT NOT NULL,
    `class_id` BIGINT NOT NULL,
    `created_by` BIGINT NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `opens_at` DATETIME NOT NULL,
    `closes_at` DATETIME NOT NULL,
    `duration_minutes` INT NOT NULL,
    `max_attempts` INT NOT NULL DEFAULT 1,
    `shuffle_questions` TINYINT(1) NOT NULL DEFAULT 1,
    `shuffle_options` TINYINT(1) NOT NULL DEFAULT 1,
    `total_points` DECIMAL(8,2) NOT NULL,
    `questions_snapshot_json` LONGTEXT NOT NULL,
    `result_published_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_exam_assignments_class_window` (`class_id`, `opens_at`, `closes_at`),
    KEY `idx_exam_assignments_teacher` (`created_by`, `created_at`),
    CONSTRAINT `fk_exam_assignments_blueprint` FOREIGN KEY (`blueprint_id`) REFERENCES `exam_blueprints` (`id`),
    CONSTRAINT `fk_exam_assignments_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
    CONSTRAINT `fk_exam_assignments_user` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `exam_attempts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `assignment_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `attempt_number` INT NOT NULL,
    `status` VARCHAR(24) NOT NULL,
    `started_at` DATETIME NOT NULL,
    `deadline_at` DATETIME NOT NULL,
    `submitted_at` DATETIME NULL,
    `auto_submitted` TINYINT(1) NOT NULL DEFAULT 0,
    `question_order_json` LONGTEXT NOT NULL,
    `option_order_json` LONGTEXT NOT NULL,
    `objective_score` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `manual_score` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `total_score` DECIMAL(8,2) NULL,
    `version` BIGINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_attempt_number` (`assignment_id`, `student_id`, `attempt_number`),
    KEY `idx_exam_attempt_expiry` (`status`, `deadline_at`),
    KEY `idx_exam_attempt_student` (`student_id`, `assignment_id`),
    CONSTRAINT `fk_exam_attempt_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `exam_assignments` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_exam_attempt_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `exam_attempt_answers` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `attempt_id` BIGINT NOT NULL,
    `question_number` INT NOT NULL,
    `answer_text` TEXT NULL,
    `selected_option_key` VARCHAR(20) NULL,
    `is_correct` TINYINT(1) NULL,
    `score` DECIMAL(8,2) NULL,
    `teacher_feedback` VARCHAR(1000) NULL,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_attempt_answer` (`attempt_id`, `question_number`),
    CONSTRAINT `fk_exam_attempt_answers_attempt` FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
