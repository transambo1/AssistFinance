CREATE TABLE IF NOT EXISTS budgets (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    category_id VARCHAR(255), -- Chỉ dùng cho LIMIT
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL, -- LIMIT, SAVING
    target_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    current_amount DECIMAL(19, 4) DEFAULT 0,
    start_date BIGINT,
    end_date BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, FINISHED, OVER_BUDGET

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,

    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );