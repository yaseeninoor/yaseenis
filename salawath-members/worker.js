const JSON_HEADERS = { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" };
const OTP_TTL_SECONDS = 300;
const SESSION_TTL_SECONDS = 60 * 60 * 24 * 30;
const MAX_OTP_ATTEMPTS = 5;
const MIN_RESEND_SECONDS = 60;

function json(data, status = 200, extraHeaders = JSON_HEADERS) {
  return new Response(JSON.stringify(data), { status, headers: extraHeaders });
}
function randomDigits(length = 6) {
  const values = new Uint32Array(length);
  crypto.getRandomValues(values);
  return Array.from(values, n => String(n % 10)).join("");
}
async function sha256(value) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), b => b.toString(16).padStart(2, "0")).join("");
}
function normalizePhone(value) {
  const phone = String(value || "").replace(/[\s()-]/g, "");
  return /^\+[1-9]\d{7,14}$/.test(phone) ? phone : null;
}
function bearer(request) {
  const header = request.headers.get("Authorization") || "";
  return header.startsWith("Bearer ") ? header.slice(7) : "";
}
function localDate(env) {
  // Set APP_TIMEZONE to a valid IANA zone (for example Asia/Kolkata or Asia/Dubai).
  const timezone = env.APP_TIMEZONE || "Asia/Kolkata";
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: timezone, year: "numeric", month: "2-digit", day: "2-digit"
  }).format(new Date());
}
async function sendWhatsAppOtp(env, phone, code) {
  if (!env.WHATSAPP_TOKEN || !env.WHATSAPP_PHONE_NUMBER_ID || !env.WHATSAPP_OTP_TEMPLATE) {
    throw new Error("WhatsApp OTP secrets/template are not configured");
  }
  const version = env.GRAPH_API_VERSION || "v23.0";
  const response = await fetch(`https://graph.facebook.com/${version}/${env.WHATSAPP_PHONE_NUMBER_ID}/messages`, {
    method: "POST",
    headers: { "Authorization": `Bearer ${env.WHATSAPP_TOKEN}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      messaging_product: "whatsapp",
      recipient_type: "individual",
      to: phone,
      type: "template",
      template: {
        name: env.WHATSAPP_OTP_TEMPLATE,
        language: { code: env.WHATSAPP_TEMPLATE_LANGUAGE || "en_US" },
        components: [
          { type: "body", parameters: [{ type: "text", text: code }] },
          { type: "button", sub_type: "url", index: "0", parameters: [{ type: "text", text: code }] }
        ]
      }
    })
  });
  if (!response.ok) {
    const err = await response.text();
    console.error("WhatsApp template send failed", response.status, err.slice(0, 500));
    throw new Error("OTP message could not be sent");
  }
}
async function getUser(env, token) {
  if (!token) return null;
  const hash = await sha256(token);
  return await env.AUTH_DB.prepare(
    "SELECT id, display_name, phone_e164 FROM users WHERE session_hash = ? AND session_expires_at > datetime('now')"
  ).bind(hash).first();
}
function corsHeaders(request, env) {
  const origin = request.headers.get("Origin") || "";
  const allowed = (env.ALLOWED_ORIGINS || "https://yaseenis.com,https://www.yaseenis.com").split(",").map(s => s.trim());
  return {
    ...JSON_HEADERS,
    "access-control-allow-origin": allowed.includes(origin) ? origin : "https://yaseenis.com",
    "vary": "Origin",
    "access-control-allow-methods": "GET,POST,OPTIONS",
    "access-control-allow-headers": "Content-Type,Authorization"
  };
}
export default {
  async fetch(request, env) {
    const headers = corsHeaders(request, env);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers });
    const url = new URL(request.url);
    try {
      if (request.method === "POST" && url.pathname === "/auth/request-otp") {
        const body = await request.json();
        const name = String(body.name || "").trim().replace(/\s+/g, " ").slice(0, 80);
        const phone = normalizePhone(body.phone);
        if (name.length < 2 || !phone) return json({ error: "Enter a valid name and phone number in international format, e.g. +971..." }, 400, headers);
        const prior = await env.AUTH_DB.prepare("SELECT created_at FROM otp_requests WHERE phone_e164 = ?").bind(phone).first();
        if (prior && Date.now() - Date.parse(prior.created_at + "Z") < MIN_RESEND_SECONDS * 1000) {
          return json({ error: "Please wait before requesting another OTP." }, 429, headers);
        }
        const code = randomDigits(6);
        const codeHash = await sha256(phone + ":" + code + ":" + env.OTP_PEPPER);
        await env.AUTH_DB.prepare(
          "INSERT INTO otp_requests (phone_e164, display_name, code_hash, attempts, expires_at, created_at) VALUES (?, ?, ?, 0, datetime('now', ?), datetime('now')) ON CONFLICT(phone_e164) DO UPDATE SET display_name=excluded.display_name, code_hash=excluded.code_hash, attempts=0, expires_at=excluded.expires_at, created_at=datetime('now')"
        ).bind(phone, name, codeHash, "+" + OTP_TTL_SECONDS + " seconds").run();
        try {
          await sendWhatsAppOtp(env, phone, code);
        } catch (e) {
          await env.AUTH_DB.prepare("DELETE FROM otp_requests WHERE phone_e164 = ?").bind(phone).run();
          return json({ error: "OTP delivery is not configured or failed. Please contact the site administrator." }, 503, headers);
        }
        return json({ ok: true, message: "OTP sent to WhatsApp. It expires in 5 minutes." }, 200, headers);
      }
      if (request.method === "POST" && url.pathname === "/auth/verify-otp") {
        const body = await request.json();
        const phone = normalizePhone(body.phone);
        const code = String(body.code || "").trim();
        if (!phone || !/^\d{6}$/.test(code)) return json({ error: "Enter the phone number and 6-digit OTP." }, 400, headers);
        const record = await env.AUTH_DB.prepare(
          "SELECT display_name, code_hash, attempts FROM otp_requests WHERE phone_e164 = ? AND expires_at > datetime('now')"
        ).bind(phone).first();
        if (!record || Number(record.attempts) >= MAX_OTP_ATTEMPTS) return json({ error: "OTP expired or too many attempts. Request a new OTP." }, 400, headers);
        const providedHash = await sha256(phone + ":" + code + ":" + env.OTP_PEPPER);
        if (providedHash !== record.code_hash) {
          await env.AUTH_DB.prepare("UPDATE otp_requests SET attempts = attempts + 1 WHERE phone_e164 = ?").bind(phone).run();
          return json({ error: "Incorrect OTP." }, 400, headers);
        }
        const session = crypto.randomUUID() + crypto.randomUUID();
        const sessionHash = await sha256(session);
        await env.AUTH_DB.prepare(
          "INSERT INTO users (id, display_name, phone_e164, session_hash, session_expires_at, created_at) VALUES (?, ?, ?, ?, datetime('now', ?), datetime('now')) ON CONFLICT(phone_e164) DO UPDATE SET display_name=excluded.display_name, session_hash=excluded.session_hash, session_expires_at=excluded.session_expires_at"
        ).bind(crypto.randomUUID(), record.display_name, phone, sessionHash, "+" + SESSION_TTL_SECONDS + " seconds").run();
        await env.AUTH_DB.prepare("DELETE FROM otp_requests WHERE phone_e164 = ?").bind(phone).run();
        const user = await env.AUTH_DB.prepare("SELECT id, display_name FROM users WHERE phone_e164 = ?").bind(phone).first();
        return json({ ok: true, token: session, user }, 200, headers);
      }
      if (request.method === "GET" && url.pathname === "/me") {
        const user = await getUser(env, bearer(request));
        if (!user) return json({ error: "Sign in required" }, 401, headers);
        const today = localDate(env);
        const day = await env.AUTH_DB.prepare("SELECT count FROM daily_counts WHERE user_id = ? AND count_date = ?").bind(user.id, today).first();
        const total = await env.AUTH_DB.prepare("SELECT COALESCE(SUM(count),0) AS total FROM daily_counts WHERE user_id = ?").bind(user.id).first();
        return json({ user: { id: user.id, name: user.display_name }, today: Number(day?.count || 0), total: Number(total?.total || 0), date: today }, 200, headers);
      }
      if (request.method === "POST" && url.pathname === "/count") {
        const user = await getUser(env, bearer(request));
        if (!user) return json({ error: "Sign in required" }, 401, headers);
        const today = localDate(env);
        await env.AUTH_DB.prepare(
          "INSERT INTO daily_counts (user_id, count_date, count) VALUES (?, ?, 1) ON CONFLICT(user_id, count_date) DO UPDATE SET count = count + 1"
        ).bind(user.id, today).run();
        const day = await env.AUTH_DB.prepare("SELECT count FROM daily_counts WHERE user_id = ? AND count_date = ?").bind(user.id, today).first();
        const total = await env.AUTH_DB.prepare("SELECT COALESCE(SUM(count),0) AS total FROM daily_counts WHERE user_id = ?").bind(user.id).first();
        return json({ ok: true, today: Number(day.count), total: Number(total.total), date: today }, 200, headers);
      }
      if (request.method === "GET" && url.pathname === "/history") {
        const user = await getUser(env, bearer(request));
        if (!user) return json({ error: "Sign in required" }, 401, headers);
        const rows = await env.AUTH_DB.prepare(
          "SELECT count_date AS date, count FROM daily_counts WHERE user_id = ? ORDER BY count_date DESC LIMIT 365"
        ).bind(user.id).all();
        const total = await env.AUTH_DB.prepare("SELECT COALESCE(SUM(count),0) AS total FROM daily_counts WHERE user_id = ?").bind(user.id).first();
        return json({ total: Number(total.total || 0), history: rows.results || [] }, 200, headers);
      }
      if (request.method === "GET" && url.pathname === "/leaderboard") {
        const rows = await env.AUTH_DB.prepare(
          "SELECT u.display_name AS name, COALESCE(SUM(d.count),0) AS total FROM users u LEFT JOIN daily_counts d ON d.user_id=u.id GROUP BY u.id ORDER BY total DESC LIMIT 100"
        ).all();
        return json({ users: rows.results || [] }, 200, headers);
      }
      return json({ error: "Not found" }, 404, headers);
    } catch (error) {
      console.error("Salawath member API error", error);
      return json({ error: "Service temporarily unavailable" }, 500, headers);
    }
  }
};
