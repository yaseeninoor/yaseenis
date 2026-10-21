# Salawath name-only member counter

Independent service for name-only registration. It does not replace the existing anonymous/public Salawath counter API or its database.

## Deploy
1. Create Cloudflare D1 database `yaseenis-salawath-name-members`.
2. Run `schema.sql` in its SQL console.
3. Create a separate Worker named `yaseenis-salawath-name-members`, paste `worker.js`, and add D1 binding `MEMBER_DB` to that database.
4. Set Worker variable `ALLOWED_ORIGINS` to `https://yaseenis.com,https://www.yaseenis.com`; optional `APP_TIMEZONE` is `Asia/Kolkata`.
5. Deploy and copy the Worker URL.
6. In `salawath/members.html`, replace `SET_MEMBER_WORKER_URL_HERE` with the Worker URL. Commit and test.

## API
- POST /register {name, deviceId}; one account per stored browser/device ID.
- POST /login {deviceId}; issues a fresh 30-day session for the existing account on this browser.
- GET /me (Bearer token)
- POST /count (Bearer token)
- GET /history (Bearer token)
- GET /leaderboard (aggregate display names and totals only)

## Limitations and privacy
A website cannot read a real hardware MAC address. This uses a random browser ID in localStorage; clearing site data, using private browsing, or switching browser/device can create another identity. It is a convenience lock, not strong identity verification. Do not use it for financial, legal, or high-assurance identity. Public leaderboard shows names and aggregate totals only. The browser stores a session token; use a trusted personal device and clear site data to sign out/reset locally. Add Cloudflare rate limiting and abuse monitoring before wide release. The public counter and this new member counter remain separate unless deliberately integrated later.
