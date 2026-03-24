CREATE TABLE IF NOT EXISTS recurring_transactions (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    category_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    type VARCHAR(20) NOT NULL, -- INCOME, EXPENSE
    frequency VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY, YEARLY
    every INTEGER DEFAULT 1,
    next_trigger_date BIGINT NOT NULL,
    last_trigger_date BIGINT,
    end_date BIGINT,
    auto_apply BOOLEAN DEFAULT TRUE,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,

    CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );