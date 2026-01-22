insert into users (email, password_hash, role, name)
values (
  'boss@garage.com',
  '$2a$10$rToMexRVgNyggo8JFwyuRefLv7OZv87QVocf9kFSLBmIDs0.1N8Be',
  'ROLE_BOSS',
  'Boss'
)
on conflict (email) do nothing;
