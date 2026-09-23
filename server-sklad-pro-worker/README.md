# Sklad PRO Worker

Cloudflare Worker backend for Sklad PRO alpha.

- Worker name: `sklad-pro-api`
- Production branch: `universal/alpha`
- Root directory: `/server-sklad-pro-worker`
- Firebase project: `sklad-pro-a1ec0`
- YooKassa credentials are Cloudflare secrets and are never committed to Git.
- Test YooKassa shop is used until the full payment path is verified end-to-end.

## Current commercial model

- Demo: 3 days.
- Demo keeps basic local inventory/catalog/warehouse operations available.
- Demo does not unlock cloud sync, multi-device access, exports or advanced requisitions.
- After demo expiry, existing local data remains readable and is not deleted, while changes require PRO.
- PRO: 500 RUB for 30 days.
- Renewing an active PRO period adds 30 days to the existing paid-until timestamp.

## Durable entitlement storage

Entitlements and processed payment IDs are stored in a SQLite-backed Cloudflare Durable Object class `SkladProAccount`.

The Worker creates one logical account object per verified Firebase uid. Payment grants are idempotent: the same YooKassa payment ID cannot extend PRO twice.

Durable Objects are used instead of a separate D1 database so the alpha payment backend stays self-contained and deploys from the existing Worker configuration.

## Required Cloudflare secret

- `SKLAD_YOOKASSA_SECRET_KEY`

## Non-secret server variables

- `SKLAD_YOOKASSA_SHOP_ID`
- `SKLAD_YOOKASSA_EXPECT_TEST`
- `SKLAD_PRO_MONTH_PRICE_RUB`
- `SKLAD_DEMO_DAYS`
- `FIREBASE_API_KEY`
- `FIREBASE_PROJECT_ID`
- `SKLAD_PAYMENT_RETURN_URL`

The Firebase Web API key is an app identifier, not an admin credential. The YooKassa secret is never committed.

## API

- `GET /health` — readiness without returning secrets.
- `GET /v1/plans` — server-owned public plan/price.
- `POST /v1/bootstrap` — starts the 3-day demo once for a verified Firebase account.
- `GET /v1/me` — current server entitlement.
- `POST /v1/payments` — creates a YooKassa payment for the server-owned monthly plan.
- `GET /v1/payments/status?payment_id=...` — re-reads YooKassa, validates uid/plan/amount/currency/test mode, then grants PRO if succeeded.
- `POST /v1/webhooks/yookassa` — re-reads the payment from YooKassa before applying a grant.

## Going live

1. Complete a Firebase-authenticated test payment.
2. Verify 500 RUB, currency, uid, plan and test-mode checks.
3. Verify the same payment ID cannot extend access twice.
4. Verify an active subscription extends from its current paid-until date.
5. Verify demo expiry does not delete local user data.
6. Configure the YooKassa `payment.succeeded` HTTP notification to the Worker webhook URL.
7. Replace only the YooKassa test shop ID/secret and set expected test mode to false.
8. Run one controlled real payment.
9. Only then enable the public payment flow in the release Android build.
