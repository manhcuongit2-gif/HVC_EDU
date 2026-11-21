ALTER TABLE ai_settings
    ADD COLUMN student_enabled TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN avatar_filename VARCHAR(120) NULL;

ALTER TABLE learning_materials
    ADD COLUMN ai_index_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

CREATE TABLE student_ai_learning_material_chunks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    page_number INT NULL,
    section_name VARCHAR(255) NULL,
    content LONGTEXT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_ai_material_chunk (material_id, chunk_index),
    KEY idx_student_ai_material_chunk_page (material_id, page_number),
    CONSTRAINT fk_student_ai_chunk_material FOREIGN KEY (material_id)
        REFERENCES learning_materials(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_ai_study_plans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    source_hash CHAR(64) NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_ai_study_plan_student (student_id),
    CONSTRAINT fk_student_ai_study_plan_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_ai_study_plan_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    item_key VARCHAR(160) NOT NULL,
    position INT NOT NULL,
    subject_name VARCHAR(100) NOT NULL,
    topic VARCHAR(150) NOT NULL,
    goal VARCHAR(500) NOT NULL,
    practice_prompt TEXT NOT NULL,
    material_references LONGTEXT NULL,
    progress_status VARCHAR(20) NOT NULL DEFAULT 'SUGGESTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_ai_study_plan_item (plan_id, item_key),
    KEY idx_student_ai_study_plan_item_position (plan_id, position),
    CONSTRAINT fk_student_ai_study_plan_item_plan FOREIGN KEY (plan_id)
        REFERENCES student_ai_study_plans(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
