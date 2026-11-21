ALTER TABLE `exam_blueprints`
    ADD COLUMN `preserve_question_order` BOOLEAN NOT NULL DEFAULT FALSE AFTER `scope`;
