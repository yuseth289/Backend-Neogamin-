CREATE TABLE conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id        UUID NOT NULL,
    seller_user_id  UUID NOT NULL,
    seller_id       UUID NOT NULL,
    product_id      UUID,
    last_message    TEXT,
    last_message_at TIMESTAMPTZ,
    unread_buyer    INT NOT NULL DEFAULT 0,
    unread_seller   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    UNIQUE (buyer_id, seller_id, product_id)
);

CREATE TABLE chat_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL,
    sender_role     VARCHAR(10) NOT NULL,
    content         TEXT NOT NULL,
    read_by_buyer   BOOLEAN NOT NULL DEFAULT false,
    read_by_seller  BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_conversations_buyer   ON conversations(buyer_id);
CREATE INDEX idx_conversations_seller  ON conversations(seller_id);
CREATE INDEX idx_chat_messages_conv    ON chat_messages(conversation_id);
