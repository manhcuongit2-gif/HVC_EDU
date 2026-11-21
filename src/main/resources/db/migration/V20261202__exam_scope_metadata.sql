ALTER TABLE `exam_blueprints`
    ADD COLUMN `scope` TEXT NULL AFTER `title`;

ALTER TABLE `exam_assignments`
    ADD COLUMN `scope` TEXT NULL AFTER `title`;
