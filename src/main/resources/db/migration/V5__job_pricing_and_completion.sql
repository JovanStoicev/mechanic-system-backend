alter table jobs add column parts_cost numeric(12,2);
alter table jobs add column labour_cost numeric(12,2);
alter table jobs add column completed_at timestamptz;

update jobs
set parts_cost = round(total_cost / 1.20, 2),
    labour_cost = total_cost - round(total_cost / 1.20, 2);

alter table jobs alter column parts_cost set not null;
alter table jobs alter column labour_cost set not null;
alter table jobs add constraint jobs_parts_cost_non_negative check (parts_cost >= 0);
alter table jobs add constraint jobs_labour_cost_non_negative check (labour_cost >= 0);

update jobs set completed_at = created_at where status = 'DONE';
