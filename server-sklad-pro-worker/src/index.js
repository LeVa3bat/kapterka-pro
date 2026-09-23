const FIREBASE_LOOKUP_URL =
  "https://identitytoolkit.googleapis.com/v1/accounts:lookup";

const DAY_MS = 24 * 60 * 60 * 1000;

function json(data, init = {}) {
  return new Response(JSON.stringify(data), {
    ...init,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      ...(init.headers || {})
    }
  });
}

function bearerToken(request) {
  const header = request.headers.get("authorization") || "";
  return header.startsWith("Bearer ") ? header.slice(7).trim() : "";
}

function normalizeMoney(value) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number.toFixed(2) : "";
}

function demoDays(env) {
  const value = Number(env.SKLAD_DEMO_DAYS || 3);
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 3;
}

function plans(env) {
  const month = normalizeMoney(env.SKLAD_PRO_MONTH_PRICE_RUB);
  const out = {};

  if (month) {
    out.pro_month = {
      id: "pro_month",
      title: "PRO на 30 дней",
      amount: month,
      currency: "RUB",
      durationDays: 30
    };
  }

  return out;
}

function trialFeatures() {
  return {
    coreInventory: true,
    localOperations: true,
    localCatalog: true,
    localWarehouses: true,
    cloudSync: false,
    multiDevice: false,
    exportReports: false,
    advancedRequisitions: false
  };
}

function proFeatures() {
  return {
    coreInventory: true,
    localOperations: true,
    localCatalog: true,
    localWarehouses: true,
    cloudSync: true,
    multiDevice: true,
    exportReports: true,
    advancedRequisitions: true
  };
}

function expiredFeatures() {
  return {
    coreInventory: true,
    localOperations: false,
    localCatalog: false,
    localWarehouses: false,
    cloudSync: false,
    multiDevice: false,
    exportReports: false,
    advancedRequisitions: false
  };
}

function effectiveEntitlement(raw, now = Date.now()) {
  const paidUntil = Number(raw?.paidUntil || 0);
  const demoEndsAt = Number(raw?.demoEndsAt || 0);
  const isProActive = paidUntil > now;
  const isTrialActive = !isProActive && demoEndsAt > now;
  const status = isProActive ? "pro" : (isTrialActive ? "trial" : "expired");

  return {
    status,
    isProActive,
    isTrialActive,
    demoStartedAt: Number(raw?.demoStartedAt || 0),
    demoEndsAt,
    paidUntil,
    planId: String(raw?.planId || ""),
    serverTime: now,
    features: isProActive
      ? proFeatures()
      : (isTrialActive ? trialFeatures() : expiredFeatures())
  };
}

async function requireFirebaseUser(request, env) {
  const idToken = bearerToken(request);
  if (!idToken) {
    const error = new Error("AUTH_REQUIRED");
    error.status = 401;
    throw error;
  }

  const response = await fetch(
    `${FIREBASE_LOOKUP_URL}?key=${encodeURIComponent(env.FIREBASE_API_KEY || "")}`,
    {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ idToken })
    }
  );

  if (!response.ok) {
    const error = new Error("INVALID_AUTH_TOKEN");
    error.status = 401;
    throw error;
  }

  const data = await response.json();
  const user = Array.isArray(data.users) ? data.users[0] : null;
  if (!user?.localId) {
    const error = new Error("INVALID_AUTH_TOKEN");
    error.status = 401;
    throw error;
  }
  if (user.emailVerified !== true) {
    const error = new Error("EMAIL_VERIFICATION_REQUIRED");
    error.status = 403;
    throw error;
  }

  return {
    uid: String(user.localId),
    email: String(user.email || "").trim().toLowerCase()
  };
}

function paymentCredentials(env) {
  const shopId = String(env.SKLAD_YOOKASSA_SHOP_ID || "").trim();
  const secret = String(env.SKLAD_YOOKASSA_SECRET_KEY || "").trim();
  if (!shopId || !secret) {
    const error = new Error("PAYMENT_NOT_CONFIGURED");
    error.status = 503;
    throw error;
  }
  return { shopId, secret };
}

async function yooRequest(env, path, options = {}) {
  const { shopId, secret } = paymentCredentials(env);
  const auth = btoa(`${shopId}:${secret}`);

  const response = await fetch(`https://api.yookassa.ru/v3${path}`, {
    method: options.method || "GET",
    headers: {
      authorization: `Basic ${auth}`,
      accept: "application/json",
      ...(options.body ? { "content-type": "application/json" } : {}),
      ...(options.idempotenceKey
        ? { "Idempotence-Key": options.idempotenceKey }
        : {})
    },
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const text = await response.text();
  let data = {};
  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = {};
  }

  if (!response.ok) {
    console.error("YooKassa request failed", response.status, path);
    const error = new Error("PAYMENT_PROVIDER_ERROR");
    error.status = 502;
    throw error;
  }
  return data;
}

function accountStub(env, uid) {
  const id = env.ACCOUNTS.idFromName(uid);
  return env.ACCOUNTS.get(id);
}

async function callAccount(env, uid, path, payload = null) {
  const response = await accountStub(env, uid).fetch(
    new Request(`https://account.internal${path}`, {
      method: payload == null ? "GET" : "POST",
      headers: payload == null ? {} : { "content-type": "application/json" },
      body: payload == null ? undefined : JSON.stringify(payload)
    })
  );

  const data = await response.json();
  if (!response.ok) {
    const error = new Error(String(data?.error || "ACCOUNT_STORAGE_ERROR"));
    error.status = response.status;
    throw error;
  }
  return data;
}

async function readJson(request) {
  try {
    return await request.json();
  } catch {
    const error = new Error("INVALID_JSON");
    error.status = 400;
    throw error;
  }
}

function assertExpectedPayment(env, payment, expectedUid = "") {
  const planId = String(payment.metadata?.planId || "");
  const plan = plans(env)[planId];

  if (
    String(payment.metadata?.app || "") !== "sklad-pro" ||
    !plan ||
    (expectedUid && String(payment.metadata?.uid || "") !== expectedUid)
  ) {
    const error = new Error("PAYMENT_BINDING_MISMATCH");
    error.status = 409;
    throw error;
  }

  if (
    normalizeMoney(payment.amount?.value) !== plan.amount ||
    String(payment.amount?.currency || "") !== plan.currency
  ) {
    const error = new Error("PAYMENT_AMOUNT_MISMATCH");
    error.status = 409;
    throw error;
  }

  const expectTest =
    String(env.SKLAD_YOOKASSA_EXPECT_TEST || "true").toLowerCase() === "true";
  if (Boolean(payment.test) !== expectTest) {
    const error = new Error("PAYMENT_MODE_MISMATCH");
    error.status = 409;
    throw error;
  }

  return {
    uid: String(payment.metadata?.uid || ""),
    plan
  };
}

async function bootstrap(request, env) {
  const user = await requireFirebaseUser(request, env);
  return callAccount(env, user.uid, "/bootstrap", {
    uid: user.uid,
    email: user.email,
    demoDays: demoDays(env)
  });
}

async function currentAccount(request, env) {
  const user = await requireFirebaseUser(request, env);
  return callAccount(env, user.uid, "/me");
}

async function createPayment(request, env) {
  const user = await requireFirebaseUser(request, env);
  const body = await readJson(request);
  const planId = String(body?.planId || "").trim();
  const plan = plans(env)[planId];
  if (!plan) {
    const error = new Error("INVALID_PLAN");
    error.status = 400;
    throw error;
  }

  const idempotenceKey =
    (request.headers.get("idempotence-key") || "").trim() ||
    crypto.randomUUID();

  const returnUrl =
    String(env.SKLAD_PAYMENT_RETURN_URL || "").trim() ||
    "skladpro://payment_success";

  const payment = await yooRequest(env, "/payments", {
    method: "POST",
    idempotenceKey,
    body: {
      amount: { value: plan.amount, currency: plan.currency },
      capture: true,
      confirmation: {
        type: "redirect",
        return_url: returnUrl
      },
      description: `Склад ПРО — ${plan.title}`,
      metadata: {
        app: "sklad-pro",
        uid: user.uid,
        planId: plan.id
      }
    }
  });

  const paymentId = String(payment.id || "");
  if (!paymentId) {
    const error = new Error("PAYMENT_CREATE_INVALID_RESPONSE");
    error.status = 502;
    throw error;
  }

  await callAccount(env, user.uid, "/payment-created", {
    paymentId,
    planId: plan.id,
    expectedAmount: plan.amount,
    currency: plan.currency,
    status: String(payment.status || "pending")
  });

  return {
    paymentId,
    status: String(payment.status || "pending"),
    confirmationUrl: String(payment.confirmation?.confirmation_url || "")
  };
}

async function validateAndApplyPayment(env, paymentId, expectedUid = "") {
  const payment = await yooRequest(
    env,
    `/payments/${encodeURIComponent(paymentId)}`
  );
  const { uid, plan } = assertExpectedPayment(env, payment, expectedUid);

  if (!uid) {
    const error = new Error("PAYMENT_BINDING_MISMATCH");
    error.status = 409;
    throw error;
  }

  if (
    String(payment.status || "") !== "succeeded" ||
    Boolean(payment.paid) !== true
  ) {
    return {
      paymentId,
      planId: plan.id,
      status: String(payment.status || "pending"),
      paid: Boolean(payment.paid),
      test: Boolean(payment.test),
      entitlement: (await callAccount(env, uid, "/me")).entitlement
    };
  }

  const applied = await callAccount(env, uid, "/apply-payment", {
    paymentId,
    planId: plan.id,
    durationDays: plan.durationDays,
    amount: plan.amount,
    currency: plan.currency
  });

  return {
    paymentId,
    planId: plan.id,
    status: "succeeded",
    paid: true,
    test: Boolean(payment.test),
    entitlement: applied.entitlement
  };
}

async function paymentStatus(request, env, url) {
  const user = await requireFirebaseUser(request, env);
  const paymentId = String(url.searchParams.get("payment_id") || "").trim();
  if (!paymentId) {
    const error = new Error("PAYMENT_ID_REQUIRED");
    error.status = 400;
    throw error;
  }
  return validateAndApplyPayment(env, paymentId, user.uid);
}

async function webhook(request, env) {
  const body = await readJson(request);
  const paymentId = String(body?.object?.id || "").trim();
  if (!paymentId) {
    return { received: true, ignored: true };
  }

  try {
    const result = await validateAndApplyPayment(env, paymentId);
    return {
      received: true,
      paymentId,
      status: result.status
    };
  } catch (error) {
    if (
      error?.message === "PAYMENT_BINDING_MISMATCH" ||
      error?.message === "PAYMENT_AMOUNT_MISMATCH" ||
      error?.message === "PAYMENT_MODE_MISMATCH"
    ) {
      return { received: true, ignored: true };
    }
    throw error;
  }
}

export { effectiveEntitlement, normalizeMoney, plans, assertExpectedPayment };

export class SkladProAccount {
  constructor(ctx) {
    this.ctx = ctx;
  }

  async getEntitlement() {
    return (await this.ctx.storage.get("entitlement")) || null;
  }

  async saveEntitlement(value) {
    await this.ctx.storage.put("entitlement", value);
  }

  async fetch(request) {
    const url = new URL(request.url);
    const now = Date.now();

    if (request.method === "POST" && url.pathname === "/bootstrap") {
      const body = await request.json();
      let entitlement = await this.getEntitlement();

      if (!entitlement) {
        const days = Math.max(1, Number(body?.demoDays || 3));
        entitlement = {
          uid: String(body?.uid || ""),
          email: String(body?.email || ""),
          demoStartedAt: now,
          demoEndsAt: now + days * DAY_MS,
          paidUntil: 0,
          planId: "",
          updatedAt: now
        };
        await this.saveEntitlement(entitlement);
      }

      return json({
        ok: true,
        entitlement: effectiveEntitlement(entitlement, now)
      });
    }

    if (request.method === "GET" && url.pathname === "/me") {
      const entitlement = await this.getEntitlement();
      return json({
        ok: true,
        entitlement: entitlement
          ? effectiveEntitlement(entitlement, now)
          : null
      });
    }

    if (request.method === "POST" && url.pathname === "/payment-created") {
      const body = await request.json();
      const paymentId = String(body?.paymentId || "");
      if (!paymentId) {
        return json({ ok: false, error: "PAYMENT_ID_REQUIRED" }, { status: 400 });
      }

      const key = `payment:${paymentId}`;
      const previous = (await this.ctx.storage.get(key)) || {};
      await this.ctx.storage.put(key, {
        ...previous,
        paymentId,
        planId: String(body?.planId || ""),
        expectedAmount: String(body?.expectedAmount || ""),
        currency: String(body?.currency || ""),
        status: String(body?.status || "pending"),
        grantApplied: Boolean(previous.grantApplied),
        createdAt: Number(previous.createdAt || now),
        updatedAt: now
      });

      return json({ ok: true });
    }

    if (request.method === "POST" && url.pathname === "/apply-payment") {
      const body = await request.json();
      const paymentId = String(body?.paymentId || "");
      const durationDays = Math.max(1, Number(body?.durationDays || 30));
      if (!paymentId) {
        return json({ ok: false, error: "PAYMENT_ID_REQUIRED" }, { status: 400 });
      }

      const paymentKey = `payment:${paymentId}`;
      const payment = (await this.ctx.storage.get(paymentKey)) || {
        paymentId,
        createdAt: now
      };

      let entitlement = (await this.getEntitlement()) || {
        uid: "",
        email: "",
        demoStartedAt: now,
        demoEndsAt: now,
        paidUntil: 0,
        planId: "",
        updatedAt: now
      };

      if (payment.grantApplied !== true) {
        const base = Math.max(now, Number(entitlement.paidUntil || 0));
        entitlement = {
          ...entitlement,
          paidUntil: base + durationDays * DAY_MS,
          planId: String(body?.planId || ""),
          updatedAt: now
        };

        await this.ctx.storage.put(paymentKey, {
          ...payment,
          planId: String(body?.planId || ""),
          expectedAmount: String(body?.amount || ""),
          currency: String(body?.currency || ""),
          status: "succeeded",
          grantApplied: true,
          grantedAt: now,
          updatedAt: now
        });
        await this.saveEntitlement(entitlement);
      }

      return json({
        ok: true,
        entitlement: effectiveEntitlement(entitlement, now)
      });
    }

    return json({ ok: false, error: "NOT_FOUND" }, { status: 404 });
  }
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/health") {
      return json({
        ok: true,
        service: "sklad-pro-worker",
        environment: env.SKLAD_ENV || "unknown",
        firebaseProject: env.FIREBASE_PROJECT_ID || "",
        firebaseAuthConfigured: Boolean(env.FIREBASE_API_KEY),
        paymentConfigured: Boolean(
          env.SKLAD_YOOKASSA_SHOP_ID &&
          env.SKLAD_YOOKASSA_SECRET_KEY
        ),
        plansConfigured: Object.keys(plans(env)).length > 0,
        demoDays: demoDays(env),
        entitlementStorage: Boolean(env.ACCOUNTS)
      });
    }

    if (request.method === "GET" && url.pathname === "/v1/plans") {
      return json({ ok: true, plans: Object.values(plans(env)) });
    }

    try {
      if (request.method === "POST" && url.pathname === "/v1/bootstrap") {
        return json({ ok: true, ...(await bootstrap(request, env)) });
      }

      if (request.method === "GET" && url.pathname === "/v1/me") {
        return json({ ok: true, ...(await currentAccount(request, env)) });
      }

      if (request.method === "POST" && url.pathname === "/v1/payments") {
        return json(
          { ok: true, ...(await createPayment(request, env)) },
          { status: 201 }
        );
      }

      if (
        request.method === "GET" &&
        url.pathname === "/v1/payments/status"
      ) {
        return json({ ok: true, ...(await paymentStatus(request, env, url)) });
      }

      if (
        request.method === "POST" &&
        url.pathname === "/v1/webhooks/yookassa"
      ) {
        return json({ ok: true, ...(await webhook(request, env)) });
      }

      return json({ ok: false, error: "NOT_FOUND" }, { status: 404 });
    } catch (error) {
      const status = Number(error?.status || 500);
      if (status >= 500) {
        console.error(
          "Worker request failed",
          request.method,
          url.pathname,
          error?.message || error
        );
      }
      return json(
        {
          ok: false,
          error:
            status >= 500
              ? "INTERNAL_ERROR"
              : String(error?.message || "REQUEST_FAILED")
        },
        { status }
      );
    }
  }
};
