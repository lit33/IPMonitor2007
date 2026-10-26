import { createClient } from "npm:@supabase/supabase-js@2";

const headers = { "content-type": "application/json", "cache-control": "no-store" };
const respond = (status: number, message: string) =>
  new Response(JSON.stringify({ error: message }), { status, headers });
const encoder = new TextEncoder();
async function hash(s: string) {
  return new Uint8Array(await crypto.subtle.digest("SHA-256", encoder.encode(s)));
}
async function tokenMatches(received: string, expected: string) {
  const [a, b] = await Promise.all([hash(received), hash(expected)]);
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
  return diff === 0;
}
type InputRow = { id: string; name: string; ip: string; up: boolean; latency_ms: number | null };
function validIp(ip: string): boolean {
  const parts = ip.split(".");
  return parts.length === 4 && parts.every((v) => /^\d{1,3}$/.test(v) && Number(v) <= 255);
}
function valid(row: unknown): row is InputRow {
  if (!row || typeof row !== "object") return false;
  const r = row as Record<string, unknown>;
  return typeof r.id === "string" && /^[a-zA-Z0-9_-]{1,64}$/.test(r.id) &&
    typeof r.name === "string" && r.name.length >= 1 && r.name.length <= 100 &&
    typeof r.ip === "string" && validIp(r.ip) &&
    typeof r.up === "boolean" &&
    (r.latency_ms === null || (Number.isInteger(r.latency_ms) && Number(r.latency_ms) >= 0 && Number(r.latency_ms) <= 60000));
}
Deno.serve(async (req) => {
  if (req.method !== "POST") return respond(405, "POST required");
  const secret = Deno.env.get("AGENT_TOKEN");
  const token = req.headers.get("x-agent-token") ?? "";
  if (!secret || secret.length < 32 || token.length > 512 || !(await tokenMatches(token, secret))) {
    return respond(401, "Unauthorized");
  }
  if (Number(req.headers.get("content-length") ?? "0") > 32768) return respond(413, "Too large");
  let payload: unknown;
  try { payload = await req.json(); } catch { return respond(400, "Invalid JSON"); }
  const entries = (payload as { results?: unknown })?.results;
  if (!Array.isArray(entries) || entries.length < 1 || entries.length > 100 || !entries.every(valid))
    return respond(400, "Invalid results (1-100)");
  const ids = entries.map((r) => r.id);
  if (new Set(ids).size !== ids.length) return respond(400, "Duplicate device id");
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceKey) return respond(500, "Server not configured");
  const db = createClient(supabaseUrl, serviceKey, { auth: { persistSession: false } });
  const now = new Date().toISOString();
  const { error: devicesError } = await db.from("monitor_devices").upsert(
    entries.map((r) => ({ id: r.id, private_ip: r.ip, display_name: r.name, updated_at: now }))
  );
  if (devicesError) { console.error("devices", devicesError.message); return respond(500, "Database write failed"); }
  const { error: historyError } = await db.from("monitor_history").insert(
    entries.map((r) => ({ device_id: r.id, is_up: r.up, latency_ms: r.up ? r.latency_ms : null, checked_at: now }))
  );
  if (historyError) { console.error("history", historyError.message); return respond(500, "Database write failed"); }
  const { error: statusError } = await db.from("monitor_public_status").upsert(
    entries.map((r) => ({ device_id: r.id, name: r.name, is_up: r.up, latency_ms: r.up ? r.latency_ms : null, checked_at: now, received_at: now }))
  );
  if (statusError) { console.error("public_status", statusError.message); return respond(500, "Database write failed"); }
  return new Response(JSON.stringify({ ok: true, accepted: entries.length }), { status: 200, headers });
});
