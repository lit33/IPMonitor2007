-- Apply in Supabase SQL editor. No private IP addresses are published.
create table if not exists public.monitor_devices (
 id text primary key check (id ~ '^[a-zA-Z0-9_-]{1,64}$'),
 private_ip inet not null,
 display_name text not null check (char_length(display_name) between 1 and 100),
 updated_at timestamptz not null default now()
);
create table if not exists public.monitor_history (
 id bigint generated always as identity primary key,
 device_id text not null references public.monitor_devices(id) on delete cascade,
 checked_at timestamptz not null default now(),
 is_up boolean not null,
 latency_ms integer check (latency_ms between 0 and 60000)
);
create index if not exists monitor_history_device_date_idx on public.monitor_history(device_id, checked_at desc);
create table if not exists public.monitor_public_status (
 device_id text primary key,
 name text not null,
 is_up boolean not null,
 latency_ms integer,
 checked_at timestamptz not null,
 received_at timestamptz not null default now()
);
alter table public.monitor_devices enable row level security;
alter table public.monitor_history enable row level security;
alter table public.monitor_public_status enable row level security;
revoke all on public.monitor_devices from anon, authenticated;
revoke all on public.monitor_history from anon, authenticated;
revoke all on public.monitor_public_status from anon, authenticated;
grant select on public.monitor_public_status to anon, authenticated;
drop policy if exists "Anyone can read public health" on public.monitor_public_status;
create policy "Anyone can read public health" on public.monitor_public_status
 for select to anon, authenticated using (true);
-- Edge Function writes with server-side service role. NEVER expose service-role to clients.
