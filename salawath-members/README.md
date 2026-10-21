# Salawath Member Counter — separate service

This is a separate member/OTP API. It does not replace or edit the existing public Salawath Live Counter API. The frontend is not yet connected to this service; deploy/configure and test this service before integrating it into `salawath/index.html`.

## Low-cost OTP recommendation
Use WhatsApp Cloud API with an approved WhatsApp Authentication template. Message charges and template/eligibility rules depend on Meta's current policy and recipient country. Do not assume OTP delivery is free. Never send OTPs from browser-side code.

## Cloudflare setup
1. Create a new D1 database named `yaseenis-salawath-members`.
2. Run `schema.sql` in that database's SQL console.
3. Create a new Worker named `yaseenis-salawath-members` and paste `worker.js`.
4. Add a D1 binding named `AUTH_DB` pointing to `yaseenis-salawath-members`.
5. Add Worker secrets: `WHATSAPP_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_OTP_TEMPLATE`, `OTP_PEPPER` (long random secret).
6. Optional plain-text variables: `WHATSAPP_TEMPLATE_LANGUAGE` (default `en_US`), `GRAPH_API_VERSION` (default `v23.0`), `APP_TIMEZONE` (default `Asia/Kolkata`), and `ALLOWED_ORIGINS` (`https://yaseenis.com,https://www.yaseenis.com`).
7. The WhatsApp template must be an approved Authentication template with a URL OTP button compatible with the request payload. If your template uses a different button type, adjust `sendWhatsAppOtp` to match the approved template exactly before production.

## API
- `POST /auth/request-otp` JSON: `{ "name": "Noor Yaseeni", "phone": "+9715XXXXXXXX" }`
- `POST /auth/verify-otp` JSON: `{ "phone": "+9715XXXXXXXX", "code": "123456" }`
- `GET /me` with `Authorization: Bearer <token>`
- `POST /count` with bearer token
- `GET /history` with bearer token
- `GET /leaderboard` returns display names and totals only

## Important production checks
- Add Cloudflare rate limiting / WAF rules for `/auth/request-otp` and `/auth/verify-otp`; OTP resend cooldown and attempt limits are also enforced in the database.
- Configure the exact allowed origins.
- Review the approved template's exact components and Graph API version. Do not expose `WHATSAPP_TOKEN` or `OTP_PEPPER` in HTML, GitHub Pages, or client JavaScript.
- Use HTTPS only. The session token is returned only after successful OTP verification; store it carefully and provide logout/session revocation before public launch.
- The existing anonymous counter button is not yet wired to this new member counter. Integrate only after the Worker is deployed and end-to-end tested, so existing public totals remain intact.
