const CACHE_NAME = "yaseenis-shell-v1";
const CORE_URLS = [
  "/",
  "/index.html",
  "/offline.html",
  "/site.webmanifest",
  "/assets/yaseenis-icon.svg",
  "/pdfs/index.html",
  "/Quran/Index.html",
  "/hadith/hadith.html",
  "/article/index.html",
  "/audio/list.html",
  "/Videos/list.html"
];

self.addEventListener("install", (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    await Promise.allSettled(CORE_URLS.map(async (url) => {
      try {
        const response = await fetch(url, { cache: "reload" });
        if (response.ok && response.type === "basic") await cache.put(url, response);
      } catch (_) {}
    }));
    await self.skipWaiting();
  })());
});

self.addEventListener("activate", (event) => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter((key) => key.startsWith("yaseenis-") && key !== CACHE_NAME).map((key) => caches.delete(key)));
    await self.clients.claim();
  })());
});

self.addEventListener("fetch", (event) => {
  const request = event.request;
  if (request.method !== "GET") return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (url.pathname.startsWith("/upload/") || url.pathname.startsWith("/admin") || url.pathname.startsWith("/salawath-analytics/")) return;
  if (url.pathname.toLowerCase().endsWith(".pdf")) return;

  if (request.mode === "navigate") {
    event.respondWith((async () => {
      try {
        const response = await fetch(request);
        if (response.ok) {
          const cache = await caches.open(CACHE_NAME);
          cache.put(request, response.clone()).catch(() => {});
        }
        return response;
      } catch (_) {
        const cache = await caches.open(CACHE_NAME);
        return (await cache.match(request)) || (await cache.match(url.pathname)) || (await cache.match("/offline.html"));
      }
    })());
    return;
  }

  if (["style", "script", "image", "font"].includes(request.destination)) {
    event.respondWith((async () => {
      const cache = await caches.open(CACHE_NAME);
      const cached = await cache.match(request);
      if (cached) return cached;
      try {
        const response = await fetch(request);
        if (response.ok && response.type === "basic") cache.put(request, response.clone()).catch(() => {});
        return response;
      } catch (_) {
        return cached || Response.error();
      }
    })());
  }
});