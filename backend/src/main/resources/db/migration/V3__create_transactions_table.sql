CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    category_id VARCHAR(255),
    category_name VARCHAR(255),
    amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    type VARCHAR(20) NOT NULL, -- INCOME, EXPENSE, ADJUSTMENT
    note TEXT,
    transaction_date BIGINT NOT NULL,
    image_url TEXT,
    is_auto BOOLEAN DEFAULT FALSE,

    version INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,

    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX idx_transactions_user_date ON transactions(user_id, transaction_date);
CREATE INDEX idx_transactions_deleted_at ON transactions(deleted_at) WHERE deleted_at IS NULL;