const json = (data, init = {}) =>
  new Response(JSON.stringify(data), {
    ...init,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      ...(init.headers || {})
    }
  });

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/health") {
      return json({
        ok: true,
        service: "sklad-pro-worker",
        environment: env.SKLAD_ENV || "unknown",
        firebaseProject: env.FIREBASE_PROJECT_ID || "",
        paymentConfigured: Boolean(
          env.SKLAD_YOOKASSA_SHOP_ID &&
          env.SKLAD_YOOKASSA_SECRET_KEY
        )
      });
    }

    return json({ ok: false, error: "NOT_FOUND" }, { status: 404 });
  }
};
