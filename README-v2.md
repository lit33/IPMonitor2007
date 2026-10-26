# IP Monitor 2007 — Windows / Supabase / public dashboard

Existing Android app stays intact. This is a **separate V2 prototype** on the feature branch.

## Architecture
Windows agent (ICMP ping in LAN) → HTTPS → Supabase Edge Function `ingest` → private PostgreSQL tables & public summary → static dashboard.

### 1. Set up Supabase
1. Create a Supabase project at https://supabase.com/dashboard .
2. Open **SQL Editor**, paste and execute `supabase/migrations/202610260001_monitor.sql`.
3. Generate a high-entropy random token of at least 32 characters, e.g. `python -c "import secrets; print(secrets.token_urlsafe(48))"`.
4. In Supabase CLI: `supabase login`; `supabase link --project-ref YOUR_PROJECT_REF` from repository root.
5. Run `supabase secrets set AGENT_TOKEN=YOUR_GENERATED_TOKEN` (use shell-safe quoting when needed).
6. Deploy via `supabase functions deploy ingest --no-verify-jwt`. API URL: `https://YOUR_PROJECT_REF.supabase.co/functions/v1/ingest`.
7. Supabase URL / server service-role key are available automatically to hosted Edge Functions (verify in project settings). **Never put service-role key in agent/dashboard.**

### 2. Windows agent
1. Install Python 3.10+ on a Windows PC continuously powered on and connected to the target LAN.
2. Copy `windows-agent/config.example.json` to `windows-agent/config.json`. Edit `endpoint` and `devices` (each unique `id`, IPv4, public-safe display name).
3. In PowerShell set token for **current session**: `$env:IPMONITOR_AGENT_TOKEN="YOUR_GENERATED_TOKEN"`; then `py -3 windows-agent/agent.py`. If you prefer, use `windows-agent/start-agent.bat` from that shell.
4. Verify that results appear in the Supabase Table Editor. The agent sends a batch every 30 s; it does not accept incoming connections.
5. For unattended operation, use Windows Task Scheduler with a restricted dedicated user, start-at-login or on-start trigger and secure per-user token storage (never commit token or write it to a public file). Validate the setup while logged out.

### 3. Public dashboard
1. In `dashboard/config.js`, replace placeholders with your **Project URL** and **publishable/anon** key (not `service_role`).
2. Publish the `dashboard/` directory using any static site host (Netlify, Cloudflare Pages, etc.). Or open `dashboard/index.html` locally for a basic smoke test; some browsers limit `file://` requests.
3. Verify public accessibility and the statuses. Dashboard polls every 30 seconds and treats a device as **unknown** when the latest upload is more than 120 seconds old.

### Security & behavior notes
- **Only publish devices you are comfortable revealing by name.** The public view does not expose private IPs, raw history or agent tokens.
- All three tables have RLS enabled. Only the status table has an anonymous read policy; the Edge Function uses server-side service role to write.
- `ingest` uses a shared long random header token (`x-agent-token`) over HTTPS. For production, improve with per-agent scoped tokens, rotation, rate limits, and audit logs.
- Result inserts are not fully transactional across three REST calls. Consider a PostgreSQL stored procedure for atomicity at scale.
- Ping latency currently includes Windows ping process overhead; it is an approximation. ICMP may be filtered by a firewall; lack of ICMP response is not necessarily service downtime.
- History grows indefinitely in this prototype. Add retention/partitioning for sustained deployments.
- Devices missing updates are marked **Нет данных**; this avoids falsely claiming LAN resources are up after agent disconnect.
- **No external provisioning is done by the repository change**. You must create Supabase project, configure secrets and static hosting. Do not publish service credentials in a public issue or chat.
