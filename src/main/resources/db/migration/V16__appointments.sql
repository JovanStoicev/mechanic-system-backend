alter table customer_problems
    add column appointment_at timestamptz,
    add column appointment_status varchar(20);

create index idx_customer_problems_appointment_at
    on customer_problems (appointment_at)
    where appointment_at is not null;

create index idx_customer_problems_mechanic_appointment
    on customer_problems (mechanic_id, appointment_at)
    where appointment_at is not null;
