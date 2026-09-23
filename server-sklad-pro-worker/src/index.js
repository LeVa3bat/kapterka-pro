const FIREBASE_LOOKUP_URL =
  "https://identitytoolkit.googleapis.com/v1/accounts:lookup";

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

function plans(env) {
  const month = normalizeMoney(env.SKLAD_PRO_MONTH_PRICE_RUB);
  const year = normalizeMoney(env.SKLAD_PRO_YEAR_PRICE_RUB);
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
  if (year) {
    out.pro_year = {
      id: "pro_year",
      title: "PRO на 365 дней",
      amount: year,
      currency: "RUB",
      durationDays: 365
    };
  }
  return out;
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

async function readJson(request) {
  try {
    return await request.json();
  } catch {
    const error = new Error("INVALID_JSON");
    error.status = 400;
    throw error;
  }
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

  return {
    paymentId: String(payment.id || ""),
    status: String(payment.status || "pending"),
    confirmationUrl: String(payment.confirmation?.confirmation_url || "")
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

  const payment = await yooRequest(
    env,
    `/payments/${encodeURIComponent(paymentId)}`
  );
  const planId = String(payment.metadata?.planId || "");
  const plan = plans(env)[planId];

  if (
    String(payment.metadata?.app || "") !== "sklad-pro" ||
    String(payment.metadata?.uid || "") !== user.uid ||
    !plan
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

  return {
    paymentId,
    planId,
    status: String(payment.status || "unknown"),
    paid: Boolean(payment.paid),
    test: Boolean(payment.test)
  };
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
        plansConfigured: Object.keys(plans(env)).length > 0
      });
    }

    if (request.method === "GET" && url.pathname === "/v1/plans") {
      return json({ ok: true, plans: Object.values(plans(env)) });
    }

    try {
      if (request.method === "POST" && url.pathname === "/v1/payments") {
        return json({ ok: true, ...(await createPayment(request, env)) }, { status: 201 });
      }

      if (
        request.method === "GET" &&
        url.pathname === "/v1/payments/status"
      ) {
        return json({ ok: true, ...(await paymentStatus(request, env, url)) });
      }

      return json({ ok: false, error: "NOT_FOUND" }, { status: 404 });
    } catch (error) {
      const status = Number(error?.status || 500);
      if (status >= 500) {
        console.error("Worker request failed", request.method, url.pathname, error?.message || error);
      }
      return json(
        {
          ok: false,
          error: status >= 500 ? "INTERNAL_ERROR" : String(error?.message || "REQUEST_FAILED")
        },
        { status }
      );
    }
  }
};
