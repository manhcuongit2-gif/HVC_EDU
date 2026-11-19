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
}
