# Salawath name-only member counter

Independent service for name-only registration. It does not replace the existing anonymous/public Salawath counter API or its database.

## Deploy
1. Create Cloudflare D1 database `yaseenis-salawath-name-members`.
2. Run `schema.sql` in its SQL console.
3. In Cloudflare Workers & Pages, create a Worker named `yaseenis-salawath-name-members` and deploy the contents of `worker.js`, or use Wrangler from this folder.
4. If using Wrangler, replace `REPLACE_WITH_CLOUDFLARE_D1_DATABASE_ID` in `wrangler.toml` with the D1 database ID, then run `npx wrangler deploy --config wrangler.toml` from this folder.
5. Confirm the D1 binding is named exactly `MEMBER_DB`. Set `ALLOWED_ORIGINS` to `https://yaseenis.com,https://www.yaseenis.com`; use `Asia/Kolkata` for `APP_TIMEZONE`.
6. Copy the deployed Worker URL. In `salawath/members.html`, replace `SET_MEMBER_WORKER_URL_HERE` with that URL and commit the change.
7. Test name registration, +1 count, refresh, daily history, and re-opening the page in the same browser.

## API
- POST /register {name, deviceId}; one account per stored browser/device ID.
- POST /login {deviceId}; issues a fresh 30-day session for the existing account on this browser.
- GET /me (Bearer token)
- POST /count (Bearer token)
- GET /history (Bearer token)
- GET /leaderboard (aggregate display names and totals only)

## Limitations and privacy
A website cannot read a real hardware MAC address. This uses a random browser ID in localStorage; clearing site data, using private browsing, or switching browser/device can create another identity. It is a convenience lock, not strong identity verification. Do not use it for financial, legal, or high-assurance identity. Public leaderboard shows names and aggregate totals only. The browser stores a session token; use a trusted personal device and clear site data to sign out/reset locally. Add Cloudflare rate limiting and abuse monitoring before wide release. The public counter and this new member counter remain separate unless deliberately integrated later.
