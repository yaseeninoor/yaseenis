const JSON_HEADERS = { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" };
function json(data, status = 200, headers = JSON_HEADERS) { return new Response(JSON.stringify(data), { status, headers }); }
function cors(request, env) {
  const origin = request.headers.get("Origin") || "";
  const allowed = (env.ALLOWED_ORIGINS || "https://yaseenis.com,https://www.yaseenis.com").split(",").map(s => s.trim());
  return { ...JSON_HEADERS, "access-control-allow-origin": allowed.includes(origin) ? origin : "https://yaseenis.com", "vary": "Origin", "access-control-allow-methods": "GET,POST,OPTIONS", "access-control-allow-headers": "Content-Type,Authorization" };
}
function localDate(env) {
  return new Intl.DateTimeFormat("en-CA", { timeZone: env.APP_TIMEZONE || "Asia/Kolkata", year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date());
}
function cleanName(value) { return String(value || "").trim().replace(/\s+/g, " ").slice(0, 60); }
function validDeviceId(value) { return /^[a-zA-Z0-9-]{20,80}$/.test(String(value || "")); }
async function sessionUser(request, env) {
  const h = request.headers.get("Authorization") || "";
  if (!h.startsWith("Bearer ")) return null;
  const token = h.slice(7);
  const hash = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token));
  const tokenHash = Array.from(new Uint8Array(hash), b => b.toString(16).padStart(2, "0")).join("");
  return await env.MEMBER_DB.prepare("SELECT id, display_name, device_id FROM members WHERE session_hash = ? AND session_expires_at > datetime('now')").bind(tokenHash).first();
}
async function hashToken(token) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token));
  return Array.from(new Uint8Array(digest), b => b.toString(16).padStart(2, "0")).join("");
}
export default {
  async fetch(request, env) {
    const headers = cors(request, env);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers });
    const url = new URL(request.url);
    try {
      if (request.method === "POST" && url.pathname === "/register") {
        const body = await request.json();
        const name = cleanName(body.name);
        const deviceId = String(body.deviceId || "");
        if (name.length < 2) return json({ error: "Please enter a name with at least 2 characters." }, 400, headers);
        if (!validDeviceId(deviceId)) return json({ error: "Invalid device ID. Refresh the page and try again." }, 400, headers);
        let member = await env.MEMBER_DB.prepare("SELECT id, display_name FROM members WHERE device_id = ?").bind(deviceId).first();
        const token = crypto.randomUUID() + crypto.randomUUID();
        const sessionHash = await hashToken(token);
        if (!member) {
          const id = crypto.randomUUID();
          try {
            await env.MEMBER_DB.prepare("INSERT INTO members (id, display_name, device_id, session_hash, session_expires_at, created_at) VALUES (?, ?, ?, ?, datetime('now', '+30 days'), datetime('now'))").bind(id, name, deviceId, sessionHash).run();
            member = { id, display_name: name };
          } catch (e) {
            // Another request may have registered this device at the same time.
            member = await env.MEMBER_DB.prepare("SELECT id, display_name FROM members WHERE device_id = ?").bind(deviceId).first();
            if (!member) throw e;
          }
        }
        // Name-only flow: re-issuing a session on this browser avoids a separate login screen.
        await env.MEMBER_DB.prepare("UPDATE members SET session_hash = ?, session_expires_at = datetime('now', '+30 days') WHERE id = ?").bind(sessionHash, member.id).run();
        return json({ ok: true, token, user: { id: member.id, name: member.display_name } }, 200, headers);
      }
      if (request.method === "POST" && url.pathname === "/login") {
        const body = await request.json();
        const deviceId = String(body.deviceId || "");
        if (!validDeviceId(deviceId)) return json({ error: "Invalid device ID." }, 400, headers);
        const member = await env.MEMBER_DB.prepare("SELECT id, display_name FROM members WHERE device_id = ?").bind(deviceId).first();
        if (!member) return json({ error: "No account found on this device. Please register first." }, 404, headers);
        const token = crypto.randomUUID() + crypto.randomUUID();
        await env.MEMBER_DB.prepare("UPDATE members SET session_hash = ?, session_expires_at = datetime('now', '+30 days') WHERE id = ?").bind(await hashToken(token), member.id).run();
        return json({ ok: true, token, user: { id: member.id, name: member.display_name } }, 200, headers);
      }
      if (request.method === "GET" && url.pathname === "/me") {
        const user = await sessionUser(request, env);
        if (!user) return json({ error: "Sign in required" }, 401, headers);
        const date = localDate(env);
        const day = await env.MEMBER_DB.prepare("SELECT count FROM daily_counts WHERE member_id = ? AND count_date = ?").bind(user.id, date).first();
        const total = await env.MEMBER_DB.prepare("SELECT COALESCE(SUM(count),0) AS total FROM daily_counts WHERE member_id = ?").bind(user.id).first();
        return json({ user: { id: user.id, name: user.display_name }, date, today: Number(day?.count || 0), total: Number(total?.total || 0) }, 200, headers);
      }
      if (request.method === "POST" && url.pathname === "/count") {
        const user = await sessionUser(request, env);
        if (!user) return json({ error: "Sign in required" }, 401, headers);
        const body = await request.json().catch(() => ({}));
        const amount = body.amount === undefined ? 1 : Number(body.amount);
        if (!Number.isSafeInteger(amount) || amount < 1 || amount > 10000) return json({ error: "Count amount must be an integer from 1 to 10000." }, 400, headers);
        const date = localDate(env);
        await env.MEMBER_DB.prepare("INSERT INTO daily_counts (member_id, count_date, count) VALUES (?, ?, ?) ON CONFLICT(member_id, count_date) DO UPDATE SET count = count + excluded.count").bind(user.id, date, amount).run();
        const day = await env.MEMBER_DB.prepare("SELECT count FROM daily_counts WHERE member_id = ? AND count_date = ?").bind(user.id, date).first();
        const total = await env.MEMBER_DB.prepare("SELECT COALESCE(SUM(count),0) AS total FROM daily_counts WHERE member_id = ?").bind(user.id).first();
        return json({ ok: true, date, added: amount, today: Number(day.count), total: Number(total.total) }, 200, headers);
      }
      if (request.method === "GET" && url.pathname === "/history") {
        const user = await sessionUser(request, env);
        if (!user) return json({ error: "Sign in required" }, 401, headers);
        const rows = await env.MEMBER_DB.prepare("SELECT count_date AS date, count FROM daily_counts WHERE member_id = ? ORDER BY count_date DESC LIMIT 365").bind(user.id).all();
        const total = await env.MEMBER_DB.prepare("SELECT COALESCE(SUM(count),0) AS total FROM daily_counts WHERE member_id = ?").bind(user.id).first();
        return json({ total: Number(total.total || 0), history: rows.results || [] }, 200, headers);
      }
      if (request.method === "GET" && url.pathname === "/leaderboard") {
        const rows = await env.MEMBER_DB.prepare("SELECT m.display_name AS name, COALESCE(SUM(d.count),0) AS total FROM members m LEFT JOIN daily_counts d ON d.member_id=m.id GROUP BY m.id ORDER BY total DESC LIMIT 100").all();
        return json({ users: rows.results || [] }, 200, headers);
      }
      return json({ error: "Not found" }, 404, headers);
    } catch (error) {
      console.error("Salawath name-only API error", error);
      return json({ error: "Service temporarily unavailable" }, 500, headers);
    }
  }
};
