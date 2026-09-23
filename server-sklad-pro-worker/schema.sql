-- Sklad PRO payment/entitlement storage for Cloudflare D1.
-- Apply after creating the dedicated D1 database and binding it as DB.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS entitlements (
  uid TEXT PRIMARY KEY NOT NULL,
  status TEXT NOT NULL DEFAULT 'trial',
  demo_started_at INTEGER NOT NULL,
  demo_ends_at INTEGER NOT NULL,
  plan_id TEXT NOT NULL DEFAULT '',
  paid_until INTEGER NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
  payment_id TEXT PRIMARY KEY NOT NULL,
  uid TEXT NOT NULL,
  plan_id TEXT NOT NULL,
  expected_amount TEXT NOT NULL,
  currency TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending',
  grant_applied INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  granted_at INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_payments_uid
  ON payments(uid);

CREATE INDEX IF NOT EXISTS idx_payments_status
  ON payments(status);
