ALTER TABLE email_verification_code
    ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'EMAIL_VERIFICATION';

CREATE INDEX idx_email_verification_type ON email_verification_code (email, type, created_at);
