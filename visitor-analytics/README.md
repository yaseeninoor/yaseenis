# Yaseenis visitor counter (separate from Salawath)

This is an independent Cloudflare Worker + D1 database for the main site's footer. It does not change the Salawath Live Counter worker or database.

## 1. Create the database
In Cloudflare Dashboard → Workers & Pages → D1 SQL Database, create a database named `yaseenis-visitor-stats`. Open its Console and run the contents of `schema.sql`.

## 2. Create the separate Worker
Create a new Worker named `yaseenis-visitor-stats` and paste `worker.js` into its editor. Under Settings → Bindings, add a D1 database binding:
- Variable name: `VISITOR_DB`
- Database: `yaseenis-visitor-stats`

Deploy the Worker and copy its workers.dev URL.

## 3. Connect the homepage
In the root `index.html`, replace `SET_WORKER_URL_HERE` in `VISITOR_STATS_API` with the deployed Worker URL, e.g. `https://yaseenis-visitor-stats.YOUR-SUBDOMAIN.workers.dev`. Commit the file.

## Privacy and counting
- Counts unique browser IDs (one per browser/device), not every page refresh or guaranteed unique human beings.
- Cloudflare's request metadata supplies approximate country/city. City can be unknown or inaccurate, especially with VPNs/mobile networks.
- The database stores a random browser ID, country/city, and first/last seen timestamps; it does not store raw IP addresses, names, or precise GPS.
- Visitors who clear browser storage or use another device/browser may be counted again.
- This public widget shows aggregate locations only; never expose visitor-level records.
