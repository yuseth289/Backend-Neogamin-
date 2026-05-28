ALTER TABLE conversations
    ALTER COLUMN seller_id      DROP NOT NULL,
    ALTER COLUMN seller_user_id DROP NOT NULL,
    ADD  COLUMN  direct_user_id UUID;

CREATE INDEX idx_conversations_direct_user
    ON conversations(direct_user_id)
    WHERE direct_user_id IS NOT NULL;
