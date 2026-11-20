CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    context_role VARCHAR(30),
    context_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_conversation_owner_updated (user_id, pinned, updated_at),
    CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    model VARCHAR(100),
    prompt_tokens INT,
    completion_tokens INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_message_conversation_created (conversation_id, created_at),
    CONSTRAINT fk_ai_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_usage (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT,
    role_name VARCHAR(30) NOT NULL,
    model VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_status INT,
    prompt_tokens BIGINT NOT NULL DEFAULT 0,
    completion_tokens BIGINT NOT NULL DEFAULT 0,
    reserved_tokens BIGINT NOT NULL DEFAULT 0,
    provider_remaining_requests BIGINT,
    provider_remaining_tokens BIGINT,
    error_code VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    PRIMARY KEY (id),
    KEY idx_ai_usage_created_status (created_at, status),
    KEY idx_ai_usage_user_created (user_id, created_at),
    CONSTRAINT fk_ai_usage_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_usage_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_settings (
    id INT NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    admin_enabled TINYINT(1) NOT NULL DEFAULT 0,
    teacher_enabled TINYINT(1) NOT NULL DEFAULT 0,
    parent_enabled TINYINT(1) NOT NULL DEFAULT 0,
    per_user_daily_requests INT,
    global_daily_requests INT,
    daily_token_limit BIGINT,
    max_prompt_characters INT,
    max_response_tokens INT,
    max_concurrent_requests INT,
    selected_model VARCHAR(100) NOT NULL DEFAULT 'openai/gpt-oss-120b',
    allowed_models VARCHAR(500) NOT NULL DEFAULT 'openai/gpt-oss-120b',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_ai_settings_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ai_settings (id, enabled, admin_enabled, teacher_enabled, parent_enabled)
VALUES (1, 0, 0, 0, 0)
ON DUPLICATE KEY UPDATE id = VALUES(id);
