# Yaseeni Live

A separate, mobile-first Dhikr + community voting website.

## Files and hosting
- Website path: `yaseeni-live/index.html`
- Intended Pages URL: `https://yaseenis.com/yaseeni-live/`
- Hosted as a static page by the existing GitHub Pages site. This adds a new folder only; it does not edit `salawath/` or its data.

## Important: separate database setup
The site currently opens in **Preview mode** until you connect a brand-new Firebase project. Do not reuse the Salawath or LP-Store Firebase project.

1. Open https://console.firebase.google.com/ and create a new project named `yaseeni-live`.
2. Add a Web App in Project settings and copy its Firebase configuration.
3. In Authentication → Sign-in method, enable **Anonymous**.
4. In Realtime Database, create a database and copy its database URL.
5. Replace the five placeholder values in `yaseeni-live/index.html` under `firebaseConfig` with the new project's `apiKey`, `authDomain`, `databaseURL`, `projectId`, and `appId`.
6. Set Realtime Database rules to the following starter rules (signed-in anonymous users only):

```json
{
  "rules": {
    "yaseeniLive": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

7. Commit the config change. Reload the site; the header should show **Live connected**.

## Data separation
All live data is stored under the `yaseeniLive` root in this new Firebase project's Realtime Database. It is independent of the existing Salawath website and its backend.

## Voting security note
The current front-end uses a per-browser localStorage guard to discourage repeat votes. This is not strong identity verification: users can clear browser storage or switch devices. Before using voting for official decisions, add authenticated voter identities and server-side validation (for example, Firebase Cloud Functions/App Check and stricter rules). The starter rules above are for a private community prototype, not high-stakes elections.

## Preview mode
Without Firebase configuration, the UI remains usable locally in one browser for a design/demo preview. Data in preview mode is not shared between visitors.
