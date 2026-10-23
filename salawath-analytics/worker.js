const JSON_HEADERS = { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" };
const json = (data, status = 200, headers = JSON_HEADERS) => new Response(JSON.stringify(data), { status, headers });
function headersFor(request, env) {
  const origin = request.headers.get("Origin") || "";
  const allowed = (env.ALLOWED_ORIGINS || "https://yaseenis.com,https://www.yaseenis.com").split(",").map(s => s.trim());
  return { ...JSON_HEADERS, "access-control-allow-origin": allowed.includes(origin) ? origin : "https://yaseenis.com", "vary": "Origin", "access-control-allow-methods": "GET,POST,OPTIONS", "access-control-allow-headers": "Content-Type,X-Admin-Token" };
}
const cleanName = value => String(value || "").trim().replace(/\s+/g, " ").slice(0, 60);
const validId = value => /^[a-zA-Z0-9-]{20,80}$/.test(String(value || ""));
function dateInZone(date, zone) {
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: zone || "Asia/Dubai", year: "numeric", month: "2-digit", day: "2-digit" }).formatToParts(date);
  const get = type => parts.find(p => p.type === type)?.value;
  return `${get("year")}-${get("month")}-${get("day")}`;
}
function adminTokenStatus(request, env) {
  const expected = String(env.ADMIN_TOKEN || "").trim();
  const actual = String(request.headers.get("X-Admin-Token") || "").trim();
  if (expected.length < 24) return "ADMIN_TOKEN is missing or shorter than 24 characters in this Worker. Re-run the deploy workflow and verify its Set analytics admin token step.";
  if (!actual) return "Enter the ADMIN_TOKEN value from GitHub Secret SALAWATH_ANALYTICS_ADMIN_TOKEN.";
  if (actual.length !== expected.length || actual !== expected) return "Token mismatch: the dashboard token does not match this Worker ADMIN_TOKEN. Re-run the latest deploy workflow and use the exact GitHub Secret value.";
  return "";
}
export default {
  async fetch(request, env) {
    const headers = headersFor(request, env);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers });
    const url = new URL(request.url);
    try {
      if (request.method === "GET" && url.pathname === "/") return json({ service: "Yaseenis Salawath Analytics", status: "ok", endpoints: ["/api/heartbeat", "/api/event", "/api/admin/online", "/api/admin/report"] }, 200, headers);
      if (!env.DB) return json({ error: "D1 binding DB is missing." }, 503, headers);
      if (request.method === "POST" && ["/api/heartbeat", "/api/event"].includes(url.pathname)) {
        const body = await request.json().catch(() => ({}));
        const visitorId = String(body.visitorId || "");
        const name = cleanName(body.name);
        if (!validId(visitorId)) return json({ error: "Invalid Visitor ID." }, 400, headers);
        const now = new Date().toISOString();
        await env.DB.prepare(`INSERT INTO salawath_presence (visitor_id, display_name, first_seen, last_seen)
          VALUES (?, ?, ?, ?)
          ON CONFLICT(visitor_id) DO UPDATE SET
            display_name = CASE WHEN excluded.display_name <> '' THEN excluded.display_name ELSE salawath_presence.display_name END,
            last_seen = excluded.last_seen`).bind(visitorId, name, now, now).run();
        if (url.pathname === "/api/event") {
          const amount = body.amount === undefined ? 1 : Number(body.amount);
          if (!Number.isSafeInteger(amount) || amount < 1 || amount > 10000) return json({ error: "Invalid count amount." }, 400, headers);
          const eventDate = dateInZone(new Date(), env.APP_TIMEZONE || "Asia/Dubai");
          await env.DB.prepare("INSERT INTO salawath_events (visitor_id, amount, event_date, created_at) VALUES (?, ?, ?, ?)").bind(visitorId, amount, eventDate, now).run();
          return json({ ok: true, recorded: amount, date: eventDate }, 200, headers);
        }
        return json({ ok: true, lastSeen: now }, 200, headers);
      }
      if (request.method === "GET" && url.pathname === "/api/public/report") {
        const date = /^\d{4}-\d{2}-\d{2}$/.test(url.searchParams.get("date") || "") ? url.searchParams.get("date") : dateInZone(new Date(), env.APP_TIMEZONE || "Asia/Dubai");
        const summary = await env.DB.prepare("SELECT COALESCE(SUM(amount),0) AS totalSalawath, COUNT(*) AS tapEvents, COUNT(DISTINCT visitor_id) AS uniqueVisitors FROM salawath_events WHERE event_date = ?").bind(date).first();
        const top = await env.DB.prepare("SELECT COALESCE(NULLIF(MAX(p.display_name),''), 'பெயர் பதிவு செய்யவில்லை') AS name, SUM(e.amount) AS count FROM salawath_events e LEFT JOIN salawath_presence p ON p.visitor_id=e.visitor_id WHERE e.event_date=? GROUP BY e.visitor_id ORDER BY count DESC LIMIT 10").bind(date).all();
        // created_at is stored as UTC ISO time; UAE has a fixed UTC+4 offset.
        // Group events by the minute they were recorded in Asia/Dubai.
        const minuteBreakdown = await env.DB.prepare("SELECT strftime('%H:%M', created_at, '+4 hours') AS minute, SUM(amount) AS count FROM salawath_events WHERE event_date = ? GROUP BY strftime('%H:%M', created_at, '+4 hours') ORDER BY minute ASC").bind(date).all();
        const onlineCutoff = new Date(Date.now() - 60_000).toISOString();
        const online = await env.DB.prepare("SELECT COALESCE(NULLIF(display_name,''), 'பெயர் பதிவு செய்யவில்லை') AS name, last_seen AS lastSeen FROM salawath_presence WHERE last_seen >= ? ORDER BY last_seen DESC LIMIT 100").bind(onlineCutoff).all();
        return json({ date, timezone: env.APP_TIMEZONE || "Asia/Dubai", totalSalawath: Number(summary?.totalSalawath || 0), tapEvents: Number(summary?.tapEvents || 0), uniqueVisitors: Number(summary?.uniqueVisitors || 0), onlineNow: (online.results || []).length, onlineVisitors: online.results || [], topUsers: top.results || [], minuteBreakdown: minuteBreakdown.results || [] }, 200, headers);
      }
      if (request.method === "GET" && url.pathname === "/api/admin/online") {
        const tokenError = adminTokenStatus(request, env); if (tokenError) return json({ error: tokenError }, 401, headers);
        const cutoff = new Date(Date.now() - 60_000).toISOString();
        const rows = await env.DB.prepare("SELECT visitor_id AS visitorId, display_name AS name, last_seen AS lastSeen FROM salawath_presence WHERE last_seen >= ? ORDER BY last_seen DESC LIMIT 500").bind(cutoff).all();
        return json({ asOf: new Date().toISOString(), onlineWindowSeconds: 60, users: rows.results || [] }, 200, headers);
      }
      if (request.method === "GET" && url.pathname === "/api/admin/report") {
        const tokenError = adminTokenStatus(request, env); if (tokenError) return json({ error: tokenError }, 401, headers);
        const date = /^\d{4}-\d{2}-\d{2}$/.test(url.searchParams.get("date") || "") ? url.searchParams.get("date") : dateInZone(new Date(), env.APP_TIMEZONE || "Asia/Dubai");
        const summary = await env.DB.prepare("SELECT COALESCE(SUM(amount),0) AS totalSalawath, COUNT(*) AS tapEvents, COUNT(DISTINCT visitor_id) AS uniqueVisitors FROM salawath_events WHERE event_date = ?").bind(date).first();
        const top = await env.DB.prepare("SELECT e.visitor_id AS visitorId, COALESCE(NULLIF(MAX(p.display_name),''), 'பெயர் பதிவு செய்யவில்லை') AS name, SUM(e.amount) AS count FROM salawath_events e LEFT JOIN salawath_presence p ON p.visitor_id=e.visitor_id WHERE e.event_date=? GROUP BY e.visitor_id ORDER BY count DESC LIMIT 20").bind(date).all();
        const onlineCutoff = new Date(Date.now() - 60_000).toISOString();
        const online = await env.DB.prepare("SELECT COUNT(*) AS count FROM salawath_presence WHERE last_seen >= ?").bind(onlineCutoff).first();
        return json({ date, timezone: env.APP_TIMEZONE || "Asia/Dubai", totalSalawath: Number(summary?.totalSalawath || 0), tapEvents: Number(summary?.tapEvents || 0), uniqueVisitors: Number(summary?.uniqueVisitors || 0), onlineNow: Number(online?.count || 0), topUsers: top.results || [] }, 200, headers);
      }
      return json({ error: "Not found" }, 404, headers);
    } catch (error) {
      console.error("Salawath analytics error", error);
      return json({ error: "Analytics service temporarily unavailable." }, 500, headers);
    }
  }
};