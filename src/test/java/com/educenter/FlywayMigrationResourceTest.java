package com.educenter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationResourceTest {

    @Test
    void additiveMigrationIncludesMissingFeatureTablesAndTeacherAdminChat() throws IOException {
        String resourcePath = "db/migration/V20261119__materials_and_teacher_admin_chat.sql";
        try (var input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertTrue(input != null, "Flyway migration must be available on the application classpath");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `learning_materials`"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `parent_teacher_conversations`"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `parent_teacher_messages`"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `teacher_admin_conversations`"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `teacher_admin_messages`"));
            assertTrue(!sql.contains("DROP TABLE"), "Migration must preserve existing data");
        }
    }

    @Test
    void repairMigrationRestoresFeatureTablesAndAddsUnreadMarkers() throws IOException {
        String resourcePath = "db/migration/V20261120__repair_chat_materials_and_read_markers.sql";
        try (var input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertTrue(input != null, "Repair migration must be available on the application classpath");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `learning_materials`"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `parent_teacher_conversations`"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `teacher_admin_conversations`"));
            assertTrue(sql.contains("parent_last_read_message_id"));
            assertTrue(sql.contains("teacher_last_read_message_id"));
            assertTrue(sql.contains("admin_last_read_message_id"));
            assertTrue(!sql.contains("DROP TABLE"), "Repair migration must preserve existing data");
        }
    }

    @Test
    void teacherPayRateMigrationStoresClassSpecificEffectiveDates() throws IOException {
        String resourcePath = "db/migration/V20261121__teacher_class_pay_rates.sql";
        try (var input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertTrue(input != null, "Teacher pay migration must be available on the application classpath");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `teacher_class_pay_rates`"));
            assertTrue(sql.contains("UNIQUE KEY `uk_teacher_class_pay_rate_effective` (`class_id`, `effective_from`)"));
        }
    }

    @Test
    void studentPortalMigrationLinksExistingProfilesAndSubscriptionsAdditively() throws IOException {
        String resourcePath = "db/migration/V20261130__student_portal_and_multi_account_push.sql";
        try (var input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertTrue(input != null, "Student portal migration must be available on the application classpath");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("'ROLE_STUDENT'"));
            assertTrue(sql.contains("ADD COLUMN `user_id` BIGINT NULL"));
            assertTrue(sql.contains("ADD COLUMN `submission_text` TEXT NULL"));
            assertTrue(sql.contains("CREATE TABLE `push_subscription_users`"));
            assertTrue(sql.contains("SELECT `id`, `user_id`, `active`, `created_at`, `last_seen_at`"));
            assertTrue(!sql.contains("DROP TABLE"), "Migration must preserve existing tables and data");
        }
    }

    @Test
    void onlineExamMigrationAddsSeparateExamTablesWithoutDroppingExistingData() throws IOException {
        String resourcePath = "db/migration/V20261201__online_exam_module.sql";
        try (var input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertTrue(input != null, "Online exam migration must be available on the application classpath");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("CREATE TABLE `exam_questions`"));
            assertTrue(sql.contains("CREATE TABLE `exam_assignments`"));
            assertTrue(sql.contains("CREATE TABLE `exam_attempts`"));
            assertTrue(sql.contains("CREATE TABLE `exam_attempt_answers`"));
            assertTrue(sql.contains("UNIQUE KEY `uk_exam_attempt_number`"));
            assertTrue(!sql.contains("DROP TABLE"), "Exam migration must preserve existing tables and data");
            assertTrue(!sql.contains("ALTER TABLE `grades`"), "Exam scores must remain separate from Grade");
        }
    }

    @Test
    void studentAiAndAccountSettingsMigrationIsAdditive() throws IOException {
        String resourcePath = "db/migration/V20261205__student_ai_personalization_and_account_settings.sql";
        try (var input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertTrue(input != null, "Student AI migration must be available on the application classpath");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("ADD COLUMN student_enabled"));
            assertTrue(sql.contains("ADD COLUMN avatar_filename"));
            assertTrue(sql.contains("CREATE TABLE student_ai_learning_material_chunks"));
            assertTrue(sql.contains("CREATE TABLE student_ai_study_plans"));
            assertTrue(sql.contains("CREATE TABLE student_ai_study_plan_items"));
            assertTrue(!sql.contains("DROP TABLE"), "Migration must preserve existing tables and data");
        }
    }
}
