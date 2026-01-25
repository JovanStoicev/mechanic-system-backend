create table cars (
  id bigserial primary key,
  brand varchar(100) not null,
  model varchar(100) not null,
  vin varchar(17) not null unique,
  engine_type varchar(100) not null,
  horse_power int not null check (horse_power >= 0),
  created_at timestamptz not null default now()
);

create table parts (
  id bigserial primary key,
  name varchar(200) not null unique,
  price numeric(12,2) not null check (price >= 0),
  stock_qty int not null check (stock_qty >= 0),
  created_at timestamptz not null default now()
);
