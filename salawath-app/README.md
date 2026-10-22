# Yaseenis Salawath Mubarak App

This is a Capacitor mobile wrapper for the existing live Salawath website. It intentionally loads `https://yaseenis.com/salawath/` so the current counter, visitor identity, leaderboard, and existing database/API integration continue to be handled by the existing website. No counter data or database schema is changed by this app wrapper.

## Android APK build

The GitHub Actions workflow **Build Salawath Android APK** generates the Android native project and builds a test APK. Open Actions in the repository, select the workflow, and run it with **Run workflow**. Download the `salawath-android-apk` artifact from a successful run.

## iPhone / App Store

The iOS project can be generated with `npm ci && npx cap add ios`. Installing on a real iPhone or distributing through TestFlight/App Store requires Apple signing credentials and an Apple Developer account. These must be configured by the repository owner; do not commit signing certificates or private keys.

## Important

The app needs an internet connection because it loads the live website. This first version is a native shell around the existing site, not an offline counter. The site remains the source of truth for counts.
