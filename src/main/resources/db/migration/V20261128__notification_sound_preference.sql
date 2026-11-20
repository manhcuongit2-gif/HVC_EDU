SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notification_outbox' AND column_name = 'sound_enabled') = 0,
    'ALTER TABLE `notification_outbox` ADD COLUMN `sound_enabled` TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE notification_sound_stmt FROM @ddl; EXECUTE notification_sound_stmt; DEALLOCATE PREPARE notification_sound_stmt;
