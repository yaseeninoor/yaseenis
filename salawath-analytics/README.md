# Salawath online visitors and daily analytics

This service is intentionally separate from the existing public Salawath counter API. It records analytics from the moment the new Worker is deployed; it cannot reconstruct older visitor activity.

## What it provides
- Online now: visitors with a heartbeat in the last 60 seconds.
- Visitor ID and the name already saved by the public Salawath name-registration popup.
- Daily count events, unique visitors, and top users (Asia/Dubai time).
- Admin endpoints protected by the Cloudflare Worker secret `ADMIN_TOKEN`.

## One-time Cloudflare setup
1. In Cloudflare Dashboard, create a D1 database named `yaseenis-salawath-analytics`.
2. Copy the D1 database ID.
3. In GitHub repository Settings → Secrets and variables → Actions, add:
   - `CLOUDFLARE_API_TOKEN` — token allowed to deploy Workers and manage D1.
   - `CLOUDFLARE_ACCOUNT_ID` — your Cloudflare account ID.
   - `SALAWATH_ANALYTICS_D1_ID` — the D1 database ID from step 2.\n   - `SALAWATH_ANALYTICS_ADMIN_TOKEN` — a strong random secret, at least 24 characters; this becomes the Worker secret used by the dashboard.
4. Deploy the workflow **Deploy Salawath Analytics Worker** from Actions.
5. In Cloudflare Worker settings for `yaseenis-salawath-analytics`, add secret `ADMIN_TOKEN` (use a strong random value at least 24 characters). Keep it private.
6. The Worker URL will be `https://yaseenis-salawath-analytics.<your-workers-subdomain>.workers.dev`. Update `ANALYTICS_API_BASE` in `salawath/index.html` to that exact URL, commit, and wait for the website deploy.

## API routes
- `POST /api/heartbeat` body: `{ "visitorId": "...", "name": "..." }`
- `POST /api/event` body: `{ "visitorId": "...", "name": "...", "amount": 1 }`
- `GET /api/admin/online` header: `X-Admin-Token: <ADMIN_TOKEN>`
- `GET /api/admin/report?date=YYYY-MM-DD` header: `X-Admin-Token: <ADMIN_TOKEN>`

Do not commit any API token, account secret, or admin token to the repository. Names are shown only if visitors registered a name; otherwise the Visitor ID remains visible.
