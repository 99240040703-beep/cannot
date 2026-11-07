# IMEI Guardian — Lost Phone Recovery

Consent-based lost-device recovery dashboard with a Node.js/PostgreSQL API and an Android Kotlin companion-app starter.

## Important reality check
An IMEI is an identifier, not a GPS transmitter. This application cannot query mobile towers, carrier databases, CEIR, or a phone's IMEI to retrieve its location or online status. The dashboard receives location and heartbeat updates only from an Android app installed and authorized before the device is lost. If the phone is powered off, offline, has location disabled, or the app loses permission, no new location can be sent.

## Features
- Responsive dashboard with sign-up/sign-in, device registration, masked IMEI display, last-known location links, lost mode, location history, notifications and account deletion.
- API with JWT authentication, bcrypt password hashes, per-user ownership checks, rate limiting, IMEI Luhn validation, AES-256-GCM encrypted IMEI storage, keyed duplicate-detection digest, audit events and PostgreSQL persistence.
- Android Kotlin/Compose starter with runtime location permission flow and visible foreground location notification.
- Render Blueprint for a web service and PostgreSQL database.
- India CEIR / Sanchar Saathi recovery guidance.

## Deploy on Render
1. Push this repository to GitHub.
2. In the correct Render workspace, create a Blueprint from this repository and render.yaml.
3. Review the planned PostgreSQL and web-service resources and approve them.
4. Render generates JWT_SECRET and IMEI_ENCRYPTION_KEY and wires DATABASE_URL from the database.
5. After deployment, open the service URL. Health endpoint: /healthz.

The Blueprint requests a Singapore region and paid starter/basic database plans. Review Render pricing and the resource plan before approval.

## Local setup
Requirements: Node.js 20+ and PostgreSQL.
1. Copy .env.example to .env.
2. Set DATABASE_URL, JWT_SECRET and IMEI_ENCRYPTION_KEY.
3. Generate a local encryption key with: node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
4. Run npm install and npm start.
5. Open http://localhost:10000.
6. Run npm run check and npm test.

## Android companion app
Open android/ in Android Studio, set the API base URL in android/app/build.gradle.kts, and build the debug APK. The current Android source is a starter scaffold: permission handling and the visible foreground service are present, but account linking and authenticated location-upload methods are TODOs. Do not treat it as a finished tracking client until those are implemented, tested and reviewed. Android background location and notification permissions are subject to OS version and Google Play policy.

See docs/API.md and PRIVACY.md. Official India recovery portal: https://www.sancharsaathi.gov.in/. This project has no direct CEIR integration.