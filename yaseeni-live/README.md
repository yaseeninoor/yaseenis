# Yaseeni Live — Cloudflare D1 setup

This is a separate app from the existing `/salawath/` website. Keep the original Salawath Worker and database untouched.

## 1. Create a separate D1 database
Cloudflare Dashboard → Workers & Pages → D1 SQL Database → Create database → `yaseeni-live-db`.
Open its Console and run all of `schema.sql`.

## 2. Deploy the Worker
Create a Cloudflare Worker called `yaseeni-live-api`. Paste `worker.js` and deploy.
Settings → Bindings → Add D1 database:
- Variable name: `DB`
- Database: `yaseeni-live-db`

Settings → Variables and Secrets: create secret `ADMIN_KEY` with a long random value. Never put this key in the website source or GitHub.

## 3. Connect the site
In `index.html`, replace `https://YOUR-WORKER-NAME.YOUR-SUBDOMAIN.workers.dev` with the deployed Worker URL. Commit the change and wait for Pages deployment. Expected URL: `https://yaseenis.com/yaseeni-live/`.

## 4. What the database stores
- `dhikr`: one row per Salawath/Dhikr menu option
- `dhikr_totals`: shared total for each option
- `members`: member identity and display name
- `dhikr_counts`: each member's total for each selected Dhikr
- `count_events`: every count batch with timestamp, for daily reporting and audit

Each dropdown item has its own ID, so its totals and leaderboard are independent. Add new entries through the “Custom Dhikr” panel (the Worker checks the ADMIN_KEY).

## 5. Important launch security
The current starter creates a browser-local member ID and asks for a display name. That is useful for a private test, but it is not strong identity verification: clearing browser data or switching devices creates another member ID. Before a public launch, add OTP/email/phone authentication and rate limits. Keep admin operations behind a server-side secret; never expose it in HTML.

## 6. Import old Salawath records
The existing Salawath page talks to a separate API: `https://yaseenis-salawath-api.yaseeninoor.workers.dev`; the old analytics endpoint is separate too. The screenshot only shows a view, not the complete underlying records. Export the old database (registered members, names, per-person totals, global totals and timestamped events) from the old Cloudflare Worker/D1/KV storage before importing. Map every old counter to a separate `dhikr.id` and import into the corresponding D1 tables. Do not reset, overwrite or point the old site to the new database.

## 7. API routes
- `GET /api/dhikr`
- `POST /api/dhikr` (requires `adminKey`)
- `GET /api/stats?dhikrId=...&memberId=...`
- `POST /api/count`
- `GET /api/leaderboard?dhikrId=...`
- `GET /api/daily?dhikrId=...`

The starter voting tab is a placeholder; voting routes and D1 tables can be added in the next phase.
