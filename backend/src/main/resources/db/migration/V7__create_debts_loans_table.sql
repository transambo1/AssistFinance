CREATE TABLE IF NOT EXISTS debts_loans (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    type VARCHAR(10) NOT NULL, -- DEBT, LOAN
    total_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(19, 4) DEFAULT 0,
    remaining_amount DECIMAL(19, 4) GENERATED ALWAYS AS (total_amount - paid_amount) STORED,
    status VARCHAR(20) DEFAULT 'ONGOING', -- ONGOING, COMPLETED, OVERDUE
    start_date BIGINT,
    due_date BIGINT,
    interest_rate DECIMAL(5, 2) DEFAULT 0,
    note TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    updated_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    deleted_at BIGINT,

    CONSTRAINT fk_debts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX idx_debts_status ON debts_loans(user_id, status);