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
}
