ALTER TABLE customer_problems
    DROP CONSTRAINT customer_problems_mechanic_id_fkey;

ALTER TABLE customer_problems
    ADD CONSTRAINT customer_problems_mechanic_id_fkey
        FOREIGN KEY (mechanic_id) REFERENCES mechanics(id) ON DELETE CASCADE;
