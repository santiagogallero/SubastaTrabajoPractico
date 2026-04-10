CREATE TABLE IF NOT EXISTS email_verification_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(120) NOT NULL,
    code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_verification_email_created (email, created_at)
);
