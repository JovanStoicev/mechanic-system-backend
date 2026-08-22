ALTER TABLE jobs DROP CONSTRAINT jobs_mechanic_id_fkey;
ALTER TABLE jobs ADD CONSTRAINT jobs_mechanic_id_fkey
    FOREIGN KEY (mechanic_id) REFERENCES mechanics(id) ON DELETE CASCADE;

ALTER TABLE part_requests DROP CONSTRAINT part_requests_mechanic_id_fkey;
ALTER TABLE part_requests ADD CONSTRAINT part_requests_mechanic_id_fkey
    FOREIGN KEY (mechanic_id) REFERENCES mechanics(id) ON DELETE CASCADE;
