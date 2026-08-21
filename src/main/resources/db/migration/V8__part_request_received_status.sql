ALTER TABLE part_requests DROP CONSTRAINT part_requests_status_check;
ALTER TABLE part_requests ADD CONSTRAINT part_requests_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'RECEIVED'));

ALTER TABLE part_requests RENAME COLUMN decided_at TO updated_at;
