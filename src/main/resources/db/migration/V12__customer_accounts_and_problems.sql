ALTER TABLE customers
    ADD COLUMN user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE jobs
    ADD COLUMN estimated_minutes INT CHECK (estimated_minutes > 0);

CREATE TABLE customer_problems (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    car_id BIGINT NOT NULL REFERENCES cars(id),
    mechanic_id BIGINT NOT NULL REFERENCES mechanics(id),
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    job_id BIGINT UNIQUE REFERENCES jobs(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (status IN ('SUBMITTED', 'CONVERTED', 'CANCELLED'))
);

CREATE INDEX idx_customer_problems_customer ON customer_problems(customer_id);
CREATE INDEX idx_customer_problems_mechanic ON customer_problems(mechanic_id);
