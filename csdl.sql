CREATE DATABASE IF NOT EXISTS `educenter_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `educenter_db`;

-- CHÚ Ý: Phiên bản này giữ nguyên dữ liệu hiện có.
-- KHÔNG DROP TABLE, KHÔNG tạo lại các bảng đã tồn tại.
-- Có thể chạy trên DB mới hoặc cập nhật schema DB cũ (xem phần đồng bộ ở cuối).

-- =======================================================
-- 1. BẢNG roles (Vai trò người dùng)
-- =======================================================
CREATE TABLE IF NOT EXISTS `roles` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 2. BẢNG users (Tài khoản người dùng)
-- =======================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `full_name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) NULL,
    `address` VARCHAR(255) NULL,
    `role_id` INT NOT NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_users_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 3. BẢNG parents (Hồ sơ phụ huynh)
-- =======================================================
CREATE TABLE IF NOT EXISTS `parents` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `occupation` VARCHAR(100) NULL,
    `note` VARCHAR(500) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_parents_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 4. BẢNG teachers (Hồ sơ giáo viên)
-- =======================================================
CREATE TABLE IF NOT EXISTS `teachers` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `specialization` VARCHAR(100) NULL,
    `degree` VARCHAR(100) NULL,
    `experience` VARCHAR(255) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_teachers_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 5. BẢNG students (Hồ sơ học sinh)
-- =======================================================
CREATE TABLE IF NOT EXISTS `students` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `full_name` VARCHAR(100) NOT NULL,
    `date_of_birth` DATE NULL,
    `gender` VARCHAR(10) NULL,
    `phone` VARCHAR(20) NULL,
    `school` VARCHAR(100) NULL,
    `grade_level` VARCHAR(20) NULL,
    `parent_id` BIGINT NOT NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_students_parents` FOREIGN KEY (`parent_id`) REFERENCES `parents` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 6. BẢNG subjects (Môn học)
-- =======================================================
CREATE TABLE IF NOT EXISTS `subjects` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(500) NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 7. BẢNG rooms (Phòng học)
-- =======================================================
CREATE TABLE IF NOT EXISTS `rooms` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `capacity` INT NOT NULL,
    `location` VARCHAR(100) NULL,
    `description` VARCHAR(500) NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 8. BẢNG classes (Lớp học)
-- =======================================================
CREATE TABLE IF NOT EXISTS `classes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `subject_id` BIGINT NOT NULL,
    `teacher_id` BIGINT NOT NULL,
    `assistant_teacher_id` BIGINT NULL,
    `max_students` INT NOT NULL,
    `tuition_fee` DECIMAL(12,2) NOT NULL,
    `session_fee` DECIMAL(12,2) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `start_date` DATE NULL,
    `end_date` DATE NULL,
    `description` VARCHAR(500) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_classes_subjects` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_classes_teachers` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_classes_assistant_teacher` FOREIGN KEY (`assistant_teacher_id`) REFERENCES `teachers` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 9. BẢNG class_students (Học sinh trong lớp - Bảng trung gian N-N)
-- =======================================================
CREATE TABLE IF NOT EXISTS `class_students` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `enrolled_date` DATE NOT NULL,
    `dropped_date` DATE NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT `uk_class_student` UNIQUE (`class_id`, `student_id`),
    CONSTRAINT `fk_class_students_classes` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_class_students_students` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 10. BẢNG registrations (Đơn đăng ký học)
-- =======================================================
CREATE TABLE IF NOT EXISTS `registrations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `parent_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `class_id` BIGINT NOT NULL,
    `registration_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `note` VARCHAR(500) NULL,
    `admin_note` VARCHAR(500) NULL,
    `processed_by` BIGINT NULL,
    `processed_date` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_registrations_parents` FOREIGN KEY (`parent_id`) REFERENCES `parents` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_registrations_students` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_registrations_classes` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_registrations_admin` FOREIGN KEY (`processed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 11. BẢNG schedules (Lịch học / Thời khóa biểu)
-- =======================================================
CREATE TABLE IF NOT EXISTS `schedules` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `room_id` BIGINT NOT NULL,
    `day_of_week` INT NOT NULL, -- 2: Thứ 2, 3: Thứ 3, ..., 8: Chủ nhật
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_schedules_classes` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_schedules_rooms` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 11.1, 11.2, 11.3. BẢNG LỊCH BUỔI HỌC VÀ BÁO CÁO
-- =======================================================
CREATE TABLE IF NOT EXISTS `class_sessions` (
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

CREATE TABLE IF NOT EXISTS `session_student_reports` (
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

CREATE TABLE IF NOT EXISTS `monthly_student_reports` (
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

-- =======================================================
-- 12. BẢNG attendance (Điểm danh học sinh)
-- =======================================================
CREATE TABLE IF NOT EXISTS `attendance` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `attendance_date` DATE NOT NULL,
    `status` VARCHAR(20) NOT NULL, -- PRESENT / ABSENT / EXCUSED
    `note` VARCHAR(500) NULL,
    `created_by` BIGINT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_attendance` UNIQUE (`class_id`, `student_id`, `attendance_date`),
    CONSTRAINT `fk_attendance_classes` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_attendance_students` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_attendance_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 13. BẢNG grades (Bảng điểm kết quả học tập)
-- =======================================================
CREATE TABLE IF NOT EXISTS `grades` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `test_score` DECIMAL(4,2) NULL,
    `midterm_score` DECIMAL(4,2) NULL,
    `final_score` DECIMAL(4,2) NULL,
    `average_score` DECIMAL(4,2) NULL,
    `note` VARCHAR(500) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_grade_class_student` UNIQUE (`class_id`, `student_id`),
    CONSTRAINT `fk_grades_classes` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_grades_students` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_test_score` CHECK (`test_score` IS NULL OR (`test_score` >= 0 AND `test_score` <= 10)),
    CONSTRAINT `chk_midterm_score` CHECK (`midterm_score` IS NULL OR (`midterm_score` >= 0 AND `midterm_score` <= 10)),
    CONSTRAINT `chk_final_score` CHECK (`final_score` IS NULL OR (`final_score` >= 0 AND `final_score` <= 10))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 14. BẢNG tuition (Khoản học phí cần nộp)
-- =======================================================
CREATE TABLE IF NOT EXISTS `tuition` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `class_id` BIGINT NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL,
    `due_date` DATE NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID', -- UNPAID / PAID / PROCESSING
    `description` VARCHAR(500) NULL,
    `billing_period` DATE NULL,
    `billing_sequence` INT NULL,
    `auto_generated` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_tuition_period_sequence` (`student_id`, `class_id`, `billing_period`, `billing_sequence`),
    CONSTRAINT `fk_tuition_students` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tuition_classes` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 15. BẢNG payments (Giao dịch thanh toán học phí)
-- =======================================================
CREATE TABLE IF NOT EXISTS `payments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tuition_id` BIGINT NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL,
    `payment_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `payment_method` VARCHAR(50) NOT NULL DEFAULT 'VNPAY', -- VNPAY / CASH / TRANSFER
    `transaction_code` VARCHAR(100) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING / SUCCESS / FAILED
    `vnpay_response_code` VARCHAR(10) NULL,
    `note` VARCHAR(500) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_payments_tuition` FOREIGN KEY (`tuition_id`) REFERENCES `tuition` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =======================================================
-- 15.1. TÀI LIỆU HỌC TẬP THEO LỚP
-- =======================================================
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

-- =======================================================
-- 15.2. HỘI THOẠI RIÊNG GIỮA PHỤ HUYNH VÀ GIÁO VIÊN THEO LỚP
-- =======================================================
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

-- =======================================================
-- 15.3. HỘI THOẠI RIÊNG GIỮA GIÁO VIÊN VÀ ADMIN
-- =======================================================
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

-- =======================================================
-- 16. ĐỒNG BỘ CỘT CHO CƠ SỞ DỮ LIỆU ĐÃ CÓ DỮ LIỆU
-- =======================================================
-- Những CREATE TABLE IF NOT EXISTS ở trên chỉ tạo các bảng mới.
-- Với các bảng cũ, đoạn dưới CHỈ thêm cột / chỉ mục nếu chưa tồn tại.
-- Các ALTER TABLE không xóa dữ liệu của bảng.

DELIMITER $$
DROP PROCEDURE IF EXISTS `hvc_schema_patch_20261118`$$
CREATE PROCEDURE `hvc_schema_patch_20261118`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'classes' AND COLUMN_NAME = 'session_fee'
    ) THEN
        ALTER TABLE `classes` ADD COLUMN `session_fee` DECIMAL(12,2) NULL AFTER `tuition_fee`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_students' AND COLUMN_NAME = 'dropped_date'
    ) THEN
        ALTER TABLE `class_students` ADD COLUMN `dropped_date` DATE NULL AFTER `enrolled_date`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tuition' AND COLUMN_NAME = 'billing_period'
    ) THEN
        ALTER TABLE `tuition` ADD COLUMN `billing_period` DATE NULL AFTER `description`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tuition' AND COLUMN_NAME = 'billing_sequence'
    ) THEN
        ALTER TABLE `tuition` ADD COLUMN `billing_sequence` INT NULL AFTER `billing_period`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tuition' AND COLUMN_NAME = 'auto_generated'
    ) THEN
        ALTER TABLE `tuition` ADD COLUMN `auto_generated` TINYINT(1) NOT NULL DEFAULT 0 AFTER `billing_sequence`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tuition'
          AND INDEX_NAME = 'uk_tuition_period_sequence'
    ) THEN
        ALTER TABLE `tuition`
          ADD UNIQUE KEY `uk_tuition_period_sequence`
          (`student_id`, `class_id`, `billing_period`, `billing_sequence`);
    END IF;
END$$
DELIMITER ;

CALL `hvc_schema_patch_20261118`();
DROP PROCEDURE IF EXISTS `hvc_schema_patch_20261118`;

-- =======================================================
-- 17. KIỂM TRA SAU KHI CHẠY
-- =======================================================
SHOW COLUMNS FROM `classes` LIKE 'session_fee';
SHOW COLUMNS FROM `tuition` LIKE 'billing_period';
SHOW COLUMNS FROM `tuition` LIKE 'billing_sequence';
SHOW COLUMNS FROM `tuition` LIKE 'auto_generated';
-- Kiểm tra tồn tại đối tượng; chức năng thực tế cần được kiểm thử sau khi chạy.
SHOW COLUMNS FROM `class_students` LIKE 'dropped_date';
SHOW TABLES LIKE 'class_sessions';
SHOW TABLES LIKE 'session_student_reports';
SHOW TABLES LIKE 'monthly_student_reports';
SHOW INDEX FROM `tuition` WHERE Key_name = 'uk_tuition_period_sequence';
SHOW TABLES LIKE 'learning_materials';
SHOW TABLES LIKE 'parent_teacher_conversations';
SHOW TABLES LIKE 'parent_teacher_messages';
SHOW TABLES LIKE 'teacher_admin_conversations';
SHOW TABLES LIKE 'teacher_admin_messages';
