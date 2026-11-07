# IMEI Guardian — Lost Phone Recovery

Consent-based lost-device recovery dashboard with a Node.js/PostgreSQL API and Android Kotlin companion app.

## Important reality check
An IMEI is an identifier, not a GPS transmitter. This application cannot query mobile towers, carrier databases, CEIR, or a phone's IMEI to retrieve its location or online status. The dashboard receives location and heartbeat updates only from an Android app installed and authorized before the device is lost. If the phone is powered off, offline, has location disabled, or the app loses permission, no new location can be sent.

## Features
- Responsive dashboard with account registration/login, device registration, masked IMEI, last-known map links, lost mode, location history, notifications and account deletion.
- API with JWT authentication, bcrypt password hashes, per-user ownership checks, rate limiting, IMEI Luhn validation, AES-256-GCM encrypted IMEI storage, keyed duplicate-detection digest, audit events and PostgreSQL persistence.
- Android Kotlin/Compose app with account creation/sign-in, manual IMEI registration, runtime location permission flow and visible foreground location service that posts GPS and battery updates to the API.
- Render Blueprint for a web service and PostgreSQL database.
- India CEIR / Sanchar Saathi recovery guidance.

## Deploy on Render
1. In the correct Render workspace, create a Blueprint from this repository and render.yaml.
2. Review the planned PostgreSQL and web-service resources and approve them.
3. Render generates JWT_SECRET and IMEI_ENCRYPTION_KEY and wires DATABASE_URL from the database.
4. After deployment, open the service URL. Health endpoint: /healthz.
The Blueprint requests Singapore region and paid starter/basic database plans. Review Render pricing and plan details before approval.

## Local setup
Requirements: Node.js 20+ and PostgreSQL.
1. Copy .env.example to .env.
2. Set DATABASE_URL, JWT_SECRET and IMEI_ENCRYPTION_KEY.
3. Generate a local encryption key: node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
4. Run npm install and npm start; open http://localhost:10000.
5. Run npm run check and npm test.

## Android build
Open android/ in Android Studio. Set API_BASE_URL in android/app/build.gradle.kts to the deployed HTTPS URL. Build the debug APK. Sign in or create an account, enter the phone's IMEI manually, register it, and tap Start location sharing. The IMEI is not read from the device automatically because Android restricts access to hardware identifiers. Android 10+ may require background location permission to be enabled separately in system Settings; follow the OS prompt and Google Play policy. A persistent notification is shown while sharing.

For a production release, add encrypted local credential storage, robust offline retry/WorkManager scheduling, account recovery, per-device revocable credentials, app integrity checks and comprehensive Android instrumentation tests.

See docs/API.md and PRIVACY.md. Official India recovery portal: https://www.sancharsaathi.gov.in/. This project has no direct CEIR integration.