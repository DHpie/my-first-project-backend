-- AI Conversation table
CREATE TABLE ai_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL DEFAULT 'AI Travel Assistant',
    archived_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_conv_user_id (user_id),
    UNIQUE KEY uk_ai_conv_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI Message table
CREATE TABLE ai_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID())),
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_msg_conv FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id),
    INDEX idx_ai_msg_conv_id (conversation_id),
    UNIQUE KEY uk_ai_msg_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
