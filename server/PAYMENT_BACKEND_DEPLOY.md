# Payment / License Backend — release gate

This backend is the trust boundary for future Android versions.

## Source
- `server/yandex-cloud-function.js`

The Android app must never contain:
- YooKassa secret key;
- Firebase service-account private key;
- Telegram bot token;
- plaintext admin secret;
- admin session signing secret.

## Required server environment variables

Payment:
- `YOOKASSA_SHOP_ID=1450722`
- `YOOKASSA_SECRET_KEY=<server only>`
- `PAYMENT_AMOUNT_RUB=490`

License registry:
- `FIREBASE_PROJECT_ID=kapterka-pro`
- exactly one of:
  - `FIREBASE_SERVICE_ACCOUNT_JSON=<server only>`
  - `FIREBASE_SERVICE_ACCOUNT_B64=<server only>`

Admin:
- `ADMIN_API_SECRET_SHA256=<sha256 of admin passphrase>`
- `ADMIN_SESSION_SECRET=<random high-entropy server secret>`

License email (required for release readiness):
- `BREVO_API_KEY=<server only>`
- `EMAIL_SENDER_EMAIL=<verified sender address>`
- optional display name: `EMAIL_SENDER_NAME=Каптёрка ПРО`

Optional Telegram notifications:
- `TG_BOT_TOKEN=<server only>`
- `TG_ADMIN_CHAT_ID=<server only>`

## Android build input

Only the public backend endpoint is provided to Gradle:

```
PAYMENT_API_URL=https://<deployed-function-endpoint>
```

No other backend secret may be supplied to the APK.

## Required API behaviour

### GET/POST `?action=health`
Must return JSON with:
- `ok: true`
- `service: "kapterka-payment-api"`
- `secretConfigured: true`
- `licenseRegistryConfigured: true`
- `adminAuthConfigured: true`
- `adminSessionConfigured: true`
- `emailConfigured: true`

Health must never return secret values.

### POST `?action=create`
Input: callsign, email, fighter_id, return_url, idempotence_key.
Returns YooKassa payment id and confirmation URL.
The price is taken from server configuration, never from Android input.

### POST `?action=check`
Input: payment_id and fighter_id.
The backend reads payment status directly from YooKassa, validates the tariff and fighter binding, writes the license registry, then returns server-issued license data.

Android must not grant PRO if this call is unavailable or unverified.

### POST `?action=admin_auth`
Accepts a plaintext passphrase only over HTTPS.
The server compares SHA-256 in constant time and returns a short-lived HMAC-signed admin session token.

### POST `?action=admin_grant_license`
Requires the valid short-lived admin token.
License creation and Firestore write happen on the server.

### POST `?action=admin_delete_fighter`
Requires the valid short-lived admin token.
Only the registry record is deleted; unit data and license records are preserved.

## Mandatory live checks before release

1. HTTPS only.
2. Health returns ready status without exposing secrets.
3. Invalid admin secret returns 403.
4. Missing/invalid admin token returns 403.
5. A payment with wrong amount cannot grant a license.
6. A payment bound to another fighter cannot grant a license.
7. Repeating `check` for one succeeded payment returns the same license key and expiry.
8. Backend/Firestore failure never creates a local Android license.
9. Server logs do not print YooKassa credentials, service-account private key, admin secret or session secret.
10. Android build is rejected when `PAYMENT_API_URL` is empty for a release candidate.

## Current release status

Do not enable the new Android payment flow in production until the deployed endpoint has passed the live checks above.
Current public Android 3.4.9 / build 31 remains unchanged.

## Standalone Node / Railway

The same trusted handler can run without changing payment/license logic:

- adapter: `server/railway-server.js`
- package: `server/package.json`
- deployment config: `railway.json`

The adapter converts ordinary HTTP requests into the same event shape used by
`server/yandex-cloud-function.js`. Secrets remain environment variables and are
never committed.

Before assigning its URL to Android, the deployment must pass
`.github/workflows/android-release-backend-readiness.yml`.

Do not set `PAYMENT_API_URL` in a release APK merely because the process starts:
health, admin rejection and all backend readiness checks must pass first.
