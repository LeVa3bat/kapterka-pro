import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs/promises";

const source = await fs.readFile(
  new URL("./src/index.js", import.meta.url),
  "utf8"
);
const moduleUrl =
  "data:text/javascript;base64," + Buffer.from(source).toString("base64");
const { effectiveEntitlement, normalizeMoney, plans, assertExpectedPayment, SkladProAccount } = await import(moduleUrl);

test("monthly plan is server-owned and normalized", () => {
  assert.equal(normalizeMoney("500"), "500.00");
  assert.equal(normalizeMoney("-1"), "");
  const configured = plans({ SKLAD_PRO_MONTH_PRICE_RUB: "500" });
  assert.deepEqual(Object.keys(configured), ["pro_month"]);
  assert.equal(configured.pro_month.amount, "500.00");
  assert.equal(configured.pro_month.currency, "RUB");
  assert.equal(configured.pro_month.durationDays, 30);
});

test("three-day trial exposes only limited features", () => {
  const now = 1_000_000;
  const entitlement = effectiveEntitlement(
    {
      demoStartedAt: now - 1000,
      demoEndsAt: now + 1000,
      paidUntil: 0,
      planId: ""
    },
    now
  );

  assert.equal(entitlement.status, "trial");
  assert.equal(entitlement.isTrialActive, true);
  assert.equal(entitlement.features.coreInventory, true);
  assert.equal(entitlement.features.localOperations, true);
  assert.equal(entitlement.features.cloudSync, false);
  assert.equal(entitlement.features.multiDevice, false);
  assert.equal(entitlement.features.exportReports, false);
  assert.equal(entitlement.features.advancedRequisitions, false);
});

test("expired trial is read-only without deleting local data", () => {
  const now = 2_000_000;
  const entitlement = effectiveEntitlement(
    {
      demoStartedAt: now - 5000,
      demoEndsAt: now - 1000,
      paidUntil: 0,
      planId: ""
    },
    now
  );

  assert.equal(entitlement.status, "expired");
  assert.equal(entitlement.features.coreInventory, true);
  assert.equal(entitlement.features.localOperations, false);
  assert.equal(entitlement.features.localCatalog, false);
  assert.equal(entitlement.features.localWarehouses, false);
  assert.equal(entitlement.features.exportReports, false);
});

test("active PRO unlocks paid features", () => {
  const now = 3_000_000;
  const entitlement = effectiveEntitlement(
    {
      demoStartedAt: now - 10000,
      demoEndsAt: now - 5000,
      paidUntil: now + 30_000,
      planId: "pro_month"
    },
    now
  );

  assert.equal(entitlement.status, "pro");
  assert.equal(entitlement.features.localOperations, true);
  assert.equal(entitlement.features.cloudSync, true);
  assert.equal(entitlement.features.multiDevice, true);
  assert.equal(entitlement.features.exportReports, true);
  assert.equal(entitlement.features.advancedRequisitions, true);
});


class FakeStorage {
  constructor() {
    this.values = new Map();
  }

  async get(key) {
    return this.values.get(key);
  }

  async put(key, value) {
    this.values.set(key, structuredClone(value));
  }
}

async function body(response) {
  return JSON.parse(await response.text());
}

test("bootstrap creates demo once and does not restart it", async () => {
  const storage = new FakeStorage();
  const account = new SkladProAccount({ storage });

  const first = await body(
    await account.fetch(
      new Request("https://account.internal/bootstrap", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          uid: "u1",
          email: "user@example.com",
          demoDays: 3
        })
      })
    )
  );

  const savedFirst = await storage.get("entitlement");

  await new Promise((resolve) => setTimeout(resolve, 2));

  const second = await body(
    await account.fetch(
      new Request("https://account.internal/bootstrap", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          uid: "u1",
          email: "user@example.com",
          demoDays: 3
        })
      })
    )
  );

  const savedSecond = await storage.get("entitlement");
  assert.equal(first.ok, true);
  assert.equal(second.ok, true);
  assert.equal(savedSecond.demoStartedAt, savedFirst.demoStartedAt);
  assert.equal(savedSecond.demoEndsAt, savedFirst.demoEndsAt);
  assert.equal(savedFirst.demoEndsAt - savedFirst.demoStartedAt, 3 * 24 * 60 * 60 * 1000);
});

test("same succeeded payment cannot grant PRO twice", async () => {
  const storage = new FakeStorage();
  const account = new SkladProAccount({ storage });

  await account.fetch(
    new Request("https://account.internal/bootstrap", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        uid: "u2",
        email: "paid@example.com",
        demoDays: 3
      })
    })
  );

  const payload = {
    paymentId: "payment-123",
    planId: "pro_month",
    durationDays: 30,
    amount: "500.00",
    currency: "RUB"
  };

  const first = await body(
    await account.fetch(
      new Request("https://account.internal/apply-payment", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(payload)
      })
    )
  );
  const firstPaidUntil = first.entitlement.paidUntil;

  const second = await body(
    await account.fetch(
      new Request("https://account.internal/apply-payment", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(payload)
      })
    )
  );

  assert.equal(second.entitlement.paidUntil, firstPaidUntil);
  const payment = await storage.get("payment:payment-123");
  assert.equal(payment.grantApplied, true);
  assert.equal(payment.expectedAmount, "500.00");
});


test("payment validation binds uid, amount, currency and test mode", () => {
  const env = {
    SKLAD_PRO_MONTH_PRICE_RUB: "500.00",
    SKLAD_YOOKASSA_EXPECT_TEST: "true"
  };
  const payment = {
    test: true,
    amount: { value: "500.00", currency: "RUB" },
    metadata: {
      app: "sklad-pro",
      uid: "uid-1",
      planId: "pro_month"
    }
  };

  const valid = assertExpectedPayment(env, payment, "uid-1");
  assert.equal(valid.uid, "uid-1");
  assert.equal(valid.plan.amount, "500.00");

  assert.throws(
    () => assertExpectedPayment(env, {
      ...payment,
      amount: { value: "499.00", currency: "RUB" }
    }, "uid-1"),
    /PAYMENT_AMOUNT_MISMATCH/
  );

  assert.throws(
    () => assertExpectedPayment(env, { ...payment, test: false }, "uid-1"),
    /PAYMENT_MODE_MISMATCH/
  );

  assert.throws(
    () => assertExpectedPayment(env, payment, "another-user"),
    /PAYMENT_BINDING_MISMATCH/
  );
});

test("new payment extends an already active subscription", async () => {
  const storage = new FakeStorage();
  const account = new SkladProAccount({ storage });
  const future = Date.now() + 10 * 24 * 60 * 60 * 1000;

  await storage.put("entitlement", {
    uid: "u3",
    email: "renew@example.com",
    demoStartedAt: Date.now() - 1000,
    demoEndsAt: Date.now() - 500,
    paidUntil: future,
    planId: "pro_month",
    updatedAt: Date.now()
  });

  const response = await body(
    await account.fetch(
      new Request("https://account.internal/apply-payment", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          paymentId: "payment-renew",
          planId: "pro_month",
          durationDays: 30,
          amount: "500.00",
          currency: "RUB"
        })
      })
    )
  );

  const expected = future + 30 * 24 * 60 * 60 * 1000;
  assert.equal(response.entitlement.paidUntil, expected);
});
