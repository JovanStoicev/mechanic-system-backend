create table users (
  id bigserial primary key,
  email varchar(255) not null unique,
  password_hash varchar(255) not null,
  role varchar(50) not null,
  name varchar(255) not null,
  created_at timestamptz not null default now()
);

create table mechanics (
  id bigserial primary key,
  user_id bigint not null unique references users(id) on delete cascade,
  fixed_salary numeric(12,2) not null default 0
);
