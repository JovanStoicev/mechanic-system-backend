create table jobs (
  id bigserial primary key,
  car_id bigint not null references cars(id),
  mechanic_id bigint not null references mechanics(id),
  description text not null,
  total_cost numeric(12,2) not null check (total_cost >= 0),
  status varchar(20) not null,
  created_at timestamptz not null default now()
);

create index idx_jobs_car_id on jobs(car_id);
create index idx_jobs_mechanic_id on jobs(mechanic_id);

create table job_parts (
  id bigserial primary key,
  job_id bigint not null references jobs(id) on delete cascade,
  part_id bigint not null references parts(id),
  qty int not null check (qty > 0)
);

create index idx_job_parts_job_id on job_parts(job_id);
create index idx_job_parts_part_id on job_parts(part_id);
