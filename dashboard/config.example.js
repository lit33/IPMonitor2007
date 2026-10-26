// Copy to config.js, set values, then deploy dashboard/ as a static website.
// Publishable / anon key is safe to use client-side only with properly configured RLS.
// NEVER put service_role, AGENT_TOKEN, or other secrets here.
window.MONITOR_CONFIG = {
  supabaseUrl: "https://YOUR_PROJECT_REF.supabase.co",
  publishableKey: "YOUR_SUPABASE_PUBLISHABLE_OR_ANON_KEY"
};
