create table invoices (
    id bigserial primary key,
    job_id bigint not null unique references jobs(id),
    invoice_number varchar(40) not null unique,
    customer_user_id bigint,
    customer_name varchar(200) not null,
    customer_email varchar(254),
    customer_phone varchar(50),
    customer_address varchar(500),
    car_brand varchar(120) not null,
    car_model varchar(120) not null,
    vin varchar(17) not null,
    mechanic_name varchar(200) not null,
    description text not null,
    mileage integer not null,
    parts_cost numeric(12,2) not null,
    labour_cost numeric(12,2) not null,
    total_cost numeric(12,2) not null,
    issued_at timestamptz not null default now()
);

create table invoice_parts (
    id bigserial primary key,
    invoice_id bigint not null references invoices(id) on delete cascade,
    part_name varchar(200) not null,
    unit_price numeric(12,2) not null,
    quantity integer not null check (quantity > 0),
    line_total numeric(12,2) not null
);

create index idx_invoices_customer_user on invoices(customer_user_id, issued_at desc);
create index idx_invoices_issued_at on invoices(issued_at desc);
create index idx_invoice_parts_invoice on invoice_parts(invoice_id);
