SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'classes' AND column_name = 'assistant_teacher_id') = 0,
    'ALTER TABLE `classes` ADD COLUMN `assistant_teacher_id` BIGINT NULL', 'SELECT 1');
PREPARE assistant_stmt FROM @ddl; EXECUTE assistant_stmt; DEALLOCATE PREPARE assistant_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'classes' AND constraint_name = 'fk_classes_assistant_teacher') = 0,
    'ALTER TABLE `classes` ADD CONSTRAINT `fk_classes_assistant_teacher` FOREIGN KEY (`assistant_teacher_id`) REFERENCES `teachers` (`id`) ON DELETE SET NULL', 'SELECT 1');
PREPARE assistant_stmt FROM @ddl; EXECUTE assistant_stmt; DEALLOCATE PREPARE assistant_stmt;
