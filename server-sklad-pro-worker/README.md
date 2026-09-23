# Sklad PRO Worker

Cloudflare Worker backend for Sklad PRO alpha.

- Worker name: `sklad-pro-api`
- Production branch: `universal/alpha`
- Root directory: `/server-sklad-pro-worker`
- Firebase project: `sklad-pro-a1ec0`
- YooKassa credentials are Cloudflare secrets and are never committed to Git.
- Test YooKassa shop is used until the full payment path is verified end-to-end.

## Required Cloudflare secrets

- `SKLAD_YOOKASSA_SECRET_KEY`

## Required server variables

- `SKLAD_YOOKASSA_SHOP_ID`
- `FIREBASE_API_KEY`
- `FIREBASE_PROJECT_ID`
- `SKLAD_PAYMENT_RETURN_URL`

Subscription prices stay server-side. Do not hardcode a new tariff in Android.

## D1

Before enabling entitlement grants, create a dedicated D1 database and bind it to the Worker as `DB`.
Apply `schema.sql` to that database. The Worker must not grant PRO access until durable storage is connected.

## Going live

1. Complete Firebase-authenticated test payment.
2. Verify payment amount, currency, uid and plan on the server.
3. Verify duplicate payment processing does not extend access twice.
4. Verify subscription extension and expiry.
5. Replace only YooKassa test `shopId` and secret with the real shop credentials.
6. Run one controlled real payment.
7. Only then enable the public payment flow in the release Android build.
