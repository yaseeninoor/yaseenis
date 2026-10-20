export default {
  async fetch(request, env) {
    const origin = request.headers.get("Origin") || "";
    const allowedOrigin = "https://yaseenis.com";
    const cors = {
      "Access-Control-Allow-Origin": origin === allowedOrigin ? allowedOrigin : allowedOrigin,
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store"
    };
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: cors });
    const url = new URL(request.url);
    try {
      if (request.method === "POST" && url.pathname === "/visit") {
        const body = await request.json();
        const visitorId = String(body.visitorId || "");
        if (!/^[a-zA-Z0-9-]{16,80}$/.test(visitorId)) {
          return Response.json({ error: "Invalid visitor ID" }, { status: 400, headers: cors });
        }
        const country = String(request.cf?.country || "Unknown").slice(0, 80);
        const city = String(request.cf?.city || "Unknown").slice(0, 100);
        await env.VISITOR_DB.prepare(
          "INSERT INTO visitors (visitor_id, country, city, first_seen, last_seen) VALUES (?, ?, ?, datetime('now'), datetime('now')) ON CONFLICT(visitor_id) DO UPDATE SET country=excluded.country, city=excluded.city, last_seen=datetime('now')"
        ).bind(visitorId, country, city).run();
        return Response.json({ ok: true }, { headers: cors });
      }
      if (request.method === "GET" && url.pathname === "/stats") {
        const total = await env.VISITOR_DB.prepare("SELECT COUNT(*) AS total FROM visitors").first();
        const locations = await env.VISITOR_DB.prepare(
          "SELECT country, city, COUNT(*) AS visitors FROM visitors GROUP BY country, city ORDER BY visitors DESC, country, city LIMIT 50"
        ).all();
        return Response.json({
          totalVisitors: Number(total?.total || 0),
          locations: (locations.results || []).map(row => ({
            country: row.country, city: row.city, visitors: Number(row.visitors)
          }))
        }, { headers: cors });
      }
      return Response.json({ error: "Not found" }, { status: 404, headers: cors });
    } catch (error) {
      return Response.json({ error: "Analytics service unavailable" }, { status: 500, headers: cors });
    }
  }
};
