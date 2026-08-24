ALTER TABLE customer_problems
    DROP CONSTRAINT customer_problems_status_check;

UPDATE customer_problems
SET status = 'APPROVED'
WHERE status = 'CONVERTED';

ALTER TABLE customer_problems
    ADD CONSTRAINT customer_problems_status_check
        CHECK (status IN ('SUBMITTED', 'ESTIMATE_CREATED', 'APPROVED', 'REJECTED', 'CANCELLED'));

ALTER TABLE customer_problems
    ADD COLUMN rejection_reason TEXT;
