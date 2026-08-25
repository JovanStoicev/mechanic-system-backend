alter table job_parts add column unit_price numeric(12,2);

update job_parts jp
set unit_price = p.price
from parts p
where p.id = jp.part_id;

alter table job_parts alter column unit_price set not null;
