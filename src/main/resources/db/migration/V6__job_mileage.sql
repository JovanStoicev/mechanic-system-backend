alter table jobs add column mileage integer;
update jobs set mileage = 0 where mileage is null;
alter table jobs alter column mileage set not null;
alter table jobs add constraint jobs_mileage_non_negative check (mileage >= 0);

create index idx_jobs_car_mileage on jobs(car_id, mileage desc);
