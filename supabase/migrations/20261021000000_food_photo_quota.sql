-- Daily limits for the food photo estimate, so one user (or a script making anonymous
-- users) cannot use up the free Gemini quota. Nothing else about users is stored.

create table if not exists public.food_photo_usage (
  user_id uuid not null references auth.users (id) on delete cascade,
  day date not null,
  count integer not null default 0,
  primary key (user_id, day)
);

create table if not exists public.food_photo_daily_total (
  day date primary key,
  count integer not null default 0
);

-- No policies: apps cannot read or change these tables directly.
alter table public.food_photo_usage enable row level security;
alter table public.food_photo_daily_total enable row level security;

-- Counts one photo for the signed-in caller and says whether it is within both limits.
-- Called by the food-photo function with the caller's own token.
create or replace function public.consume_food_photo_quota(per_user_limit integer, total_limit integer)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  user_count integer;
  all_count integer;
begin
  if auth.uid() is null then
    raise exception 'not signed in' using errcode = '28000';
  end if;

  insert into food_photo_usage (user_id, day, count)
  values (auth.uid(), current_date, 1)
  on conflict (user_id, day) do update set count = food_photo_usage.count + 1
  returning count into user_count;

  insert into food_photo_daily_total (day, count)
  values (current_date, 1)
  on conflict (day) do update set count = food_photo_daily_total.count + 1
  returning count into all_count;

  -- Hard caps, whatever the caller passes.
  return user_count <= least(per_user_limit, 100) and all_count <= least(total_limit, 1000);
end;
$$;

revoke all on function public.consume_food_photo_quota(integer, integer) from public, anon;
grant execute on function public.consume_food_photo_quota(integer, integer) to authenticated;

-- Old counts are not needed.
create or replace function public.prune_food_photo_usage()
returns void
language sql
security definer
set search_path = public
as $$
  delete from food_photo_usage where day < current_date - 7;
  delete from food_photo_daily_total where day < current_date - 7;
$$;

revoke all on function public.prune_food_photo_usage() from public, anon, authenticated;
