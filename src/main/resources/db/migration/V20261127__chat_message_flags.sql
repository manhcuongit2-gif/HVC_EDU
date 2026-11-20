SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_messages' AND column_name = 'important') = 0,
    'ALTER TABLE `chat_messages` ADD COLUMN `important` TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE chat_flag_stmt FROM @ddl; EXECUTE chat_flag_stmt; DEALLOCATE PREPARE chat_flag_stmt;
