CREATE TABLE followed_sellers (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id   UUID        NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, seller_id)
);

CREATE INDEX idx_followed_sellers_user   ON followed_sellers(user_id);
CREATE INDEX idx_followed_sellers_seller ON followed_sellers(seller_id);
