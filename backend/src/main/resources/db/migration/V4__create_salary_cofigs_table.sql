CREATE TABLE IF NOT EXISTS salary_configs (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    category_id VARCHAR(255),
    amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    pay_day INTEGER CHECK (pay_day BETWEEN 1 AND 31),
    last_processed BIGINT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,

    CONSTRAINT fk_salary_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );