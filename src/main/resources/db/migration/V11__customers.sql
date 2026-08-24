CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(254),
    address VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX ux_customers_email_lower
    ON customers (LOWER(email))
    WHERE email IS NOT NULL;

ALTER TABLE cars
    ADD COLUMN customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL;

CREATE INDEX idx_cars_customer_id ON cars(customer_id);
