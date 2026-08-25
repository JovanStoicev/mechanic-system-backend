ALTER TABLE jobs
    ADD COLUMN next_service_date date,
    ADD COLUMN next_service_mileage integer;

ALTER TABLE jobs
    ADD CONSTRAINT chk_next_service_mileage_positive
        CHECK (next_service_mileage IS NULL OR next_service_mileage > 0);
