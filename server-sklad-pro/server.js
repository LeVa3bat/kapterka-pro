const http = require('http');
const crypto = require('crypto');
const { URL } = require('url');
const { initializeApp, getApps, applicationDefault } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');

const PORT = Number(process.env.PORT || 8080);
const MAX_BODY_BYTES = 64 * 1024;
const DEMO_DAYS = Number(process.env.SKLAD_DEMO_DAYS || 7);
const EXPECTED_FIREBASE_PROJECT_ID = 'sklad-pro-a1ec0';
const VALID_PROFILE_IDS = new Set([
  'universal',
  'retail',
  'auto',
  'construction',
  'tools',
  'manufacturing',
  'food',
  'medical',
  'office_it',
  'education',
  'wholesale',
  'logistics',
  'service',
  'military'
]);

let firebaseReady = false;
let firebaseInitError = '';

function env(name) {
  return String(process.env[name] || '').trim();
}

function isExpectedFirebaseProject(projectId) {
  return String(projectId || '').trim() === EXPECTED_FIREBASE_PROJECT_ID;
}

function initFirebase() {
  if (getApps().length > 0) {
    const appProjectId = getApps()[0]?.options?.projectId || '';
    firebaseReady = isExpectedFirebaseProject(appProjectId);
    firebaseInitError = firebaseReady ? '' : 'Unexpected Firebase project';
    return;
  }

  try {
    const projectId = env('SKLAD_FIREBASE_PROJECT_ID');
    if (!isExpectedFirebaseProject(projectId)) {
      firebaseInitError = 'Unexpected or missing Firebase project';
      return;
    }

    // Cloud Run uses its service identity through Application Default Credentials.
    // Local development may use GOOGLE_APPLICATION_CREDENTIALS. Long-lived JSON
    // service-account secrets are intentionally not accepted through app env vars.
    initializeApp({
      credential: applicationDefault(),
      projectId
    });

    firebaseReady = true;
  } catch (error) {
    console.error('Firebase initialization failed:', error?.message || error);
    firebaseInitError = 'Firebase initialization failed';
    firebaseReady = false;
  }
}

initFirebase();

function plans() {
  const monthlyPrice = env('SKLAD_PRO_MONTH_PRICE_RUB');
  const yearlyPrice = env('SKLAD_PRO_YEAR_PRICE_RUB');

  const out = {};
  if (monthlyPrice) {
    out.pro_month = {
      id: 'pro_month',
      title: 'PRO на 30 дней',
      durationDays: 30,
      amount: normalizeMoney(monthlyPrice),
      currency: 'RUB'
    };
  }
  if (yearlyPrice) {
    out.pro_year = {
      id: 'pro_year',
      title: 'PRO на 365 дней',
      durationDays: 365,
      amount: normalizeMoney(yearlyPrice),
      currency: 'RUB'
    };
  }
  return out;
}

function normalizeMoney(value) {
  const number = Number(value);
  if (!Number.isFinite(number) || number <= 0) return '';
  return number.toFixed(2);
}

function paymentConfigured() {
  return Boolean(env('SKLAD_YOOKASSA_SHOP_ID') && env('SKLAD_YOOKASSA_SECRET_KEY'));
}

function json(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.statusCode = statusCode;
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.end(body);
}

function setCors(req, res) {
  const allowedOrigin = env('SKLAD_WEB_ORIGIN');
  if (allowedOrigin && req.headers.origin === allowedOrigin) {
    res.setHeader('Access-Control-Allow-Origin', allowedOrigin);
    res.setHeader('Vary', 'Origin');
  }
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,DELETE,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Authorization,Content-Type,Idempotence-Key');
}

async function readJson(req) {
  const chunks = [];
  let total = 0;
  for await (const chunk of req) {
    total += chunk.length;
    if (total > MAX_BODY_BYTES) {
      const error = new Error('PAYLOAD_TOO_LARGE');
      error.statusCode = 413;
      throw error;
    }
    chunks.push(chunk);
  }

  if (chunks.length === 0) return {};
  const raw = Buffer.concat(chunks).toString('utf8');
  if (!raw.trim()) return {};

  try {
    return JSON.parse(raw);
  } catch {
    const error = new Error('INVALID_JSON');
    error.statusCode = 400;
    throw error;
  }
}

function bearerToken(req) {
  const header = String(req.headers.authorization || '');
  if (!header.startsWith('Bearer ')) return '';
  return header.slice(7).trim();
}

function requireVerifiedEmail(decoded) {
  if (!decoded?.email || decoded.email_verified !== true) {
    const error = new Error('EMAIL_VERIFICATION_REQUIRED');
    error.statusCode = 403;
    throw error;
  }
  return decoded;
}

async function requireUser(req) {
  if (!firebaseReady) {
    const error = new Error('BACKEND_NOT_READY');
    error.statusCode = 503;
    throw error;
  }

  const token = bearerToken(req);
  if (!token) {
    const error = new Error('AUTH_REQUIRED');
    error.statusCode = 401;
    throw error;
  }

  try {
    const decoded = await getAuth().verifyIdToken(token, true);
    return requireVerifiedEmail(decoded);
  } catch (error) {
    if (error?.message === 'EMAIL_VERIFICATION_REQUIRED') throw error;
    const invalid = new Error('INVALID_AUTH_TOKEN');
    invalid.statusCode = 401;
    throw invalid;
  }
}

async function bootstrapUser(decoded) {
  const db = getFirestore();
  const userRef = db.collection('users').doc(decoded.uid);
  const entitlementRef = db.collection('entitlements').doc(decoded.uid);
  const now = Date.now();

  await db.runTransaction(async (tx) => {
    const [userSnap, entitlementSnap] = await Promise.all([
      tx.get(userRef),
      tx.get(entitlementRef)
    ]);

    if (!userSnap.exists) {
      tx.set(userRef, {
        uid: decoded.uid,
        email: decoded.email || '',
        displayName: decoded.name || '',
        createdAt: Timestamp.fromMillis(now),
        updatedAt: Timestamp.fromMillis(now)
      });
    } else {
      tx.set(userRef, {
        email: decoded.email || userSnap.data()?.email || '',
        updatedAt: Timestamp.fromMillis(now)
      }, { merge: true });
    }

    if (!entitlementSnap.exists) {
      tx.set(entitlementRef, {
        uid: decoded.uid,
        status: 'trial',
        demoStartedAt: Timestamp.fromMillis(now),
        demoEndsAt: Timestamp.fromMillis(now + DEMO_DAYS * 86400000),
        planId: '',
        paidUntil: null,
        updatedAt: Timestamp.fromMillis(now)
      });
    }
  });

  return readEntitlement(decoded.uid);
}

function effectiveEntitlement(data, now = Date.now()) {
  const demoStartedAt = timestampMillis(data.demoStartedAt);
  const demoEndsAt = timestampMillis(data.demoEndsAt);
  const paidUntil = timestampMillis(data.paidUntil);
  const isProActive = paidUntil > now;
  const isTrialActive = !isProActive && demoEndsAt > now;
  const status = isProActive ? 'pro' : (isTrialActive ? 'trial' : 'expired');

  return {
    status,
    storedStatus: data.status || '',
    planId: data.planId || '',
    demoStartedAt,
    demoEndsAt,
    paidUntil,
    isProActive,
    isTrialActive,
    serverTime: now
  };
}

async function readEntitlement(uid) {
  const snap = await getFirestore().collection('entitlements').doc(uid).get();
  if (!snap.exists) return null;
  return effectiveEntitlement(snap.data() || {});
}

function timestampMillis(value) {
  if (!value) return 0;
  if (typeof value.toMillis === 'function') return value.toMillis();
  if (value instanceof Date) return value.getTime();
  if (typeof value === 'number') return value;
  return 0;
}

async function createWorkspace(decoded, body) {
  const name = String(body?.name || '').trim();
  const profileId = String(body?.profileId || 'universal').trim();
  if (!name) {
    const error = new Error('WORKSPACE_NAME_REQUIRED');
    error.statusCode = 400;
    throw error;
  }
  if (!VALID_PROFILE_IDS.has(profileId)) {
    const error = new Error('INVALID_PROFILE_ID');
    error.statusCode = 400;
    throw error;
  }

  const workspaceId = crypto.randomUUID();
  const db = getFirestore();
  const workspaceRef = db.collection('workspaces').doc(workspaceId);
  const memberRef = workspaceRef.collection('members').doc(decoded.uid);
  const now = Timestamp.now();

  await db.runTransaction(async (tx) => {
    tx.create(workspaceRef, {
      id: workspaceId,
      name,
      profileId,
      ownerUid: decoded.uid,
      createdAt: now,
      updatedAt: now
    });
    tx.create(memberRef, {
      uid: decoded.uid,
      role: 'owner',
      email: decoded.email || '',
      createdAt: now
    });
  });

  return { workspaceId, name, profileId, role: 'owner' };
}

function normalizeDevicePayload(body) {
  const installationId = String(body?.installationId || '').trim();
  const name = String(body?.name || '').trim().slice(0, 120);
  const model = String(body?.model || '').trim().slice(0, 120);
  const appVersion = String(body?.appVersion || '').trim().slice(0, 40);

  if (!/^[A-Za-z0-9._:-]{8,128}$/.test(installationId)) {
    const error = new Error('INVALID_INSTALLATION_ID');
    error.statusCode = 400;
    throw error;
  }

  return {
    installationId,
    name,
    model,
    appVersion,
    platform: 'android'
  };
}

async function registerDevice(decoded, body) {
  const device = normalizeDevicePayload(body);
  const ref = getFirestore()
    .collection('users')
    .doc(decoded.uid)
    .collection('devices')
    .doc(device.installationId);
  const existing = await ref.get();
  const now = Timestamp.now();

  await ref.set({
    ...device,
    uid: decoded.uid,
    email: decoded.email || '',
    createdAt: existing.exists ? (existing.data()?.createdAt || now) : now,
    lastSeenAt: now
  }, { merge: true });

  return {
    ...device,
    lastSeenAt: now.toMillis()
  };
}

async function listDevices(decoded) {
  const snap = await getFirestore()
    .collection('users')
    .doc(decoded.uid)
    .collection('devices')
    .get();

  return snap.docs
    .map((doc) => {
      const data = doc.data() || {};
      return {
        installationId: doc.id,
        name: data.name || '',
        model: data.model || '',
        appVersion: data.appVersion || '',
        platform: data.platform || 'android',
        createdAt: timestampMillis(data.createdAt),
        lastSeenAt: timestampMillis(data.lastSeenAt)
      };
    })
    .sort((a, b) => b.lastSeenAt - a.lastSeenAt);
}

async function deleteDevice(decoded, installationId) {
  const cleanId = String(installationId || '').trim();
  if (!/^[A-Za-z0-9._:-]{8,128}$/.test(cleanId)) {
    const error = new Error('INVALID_INSTALLATION_ID');
    error.statusCode = 400;
    throw error;
  }

  await getFirestore()
    .collection('users')
    .doc(decoded.uid)
    .collection('devices')
    .doc(cleanId)
    .delete();

  return { installationId: cleanId, deleted: true };
}

async function yooRequest(path, options = {}) {
  if (!paymentConfigured()) {
    const error = new Error('PAYMENT_NOT_CONFIGURED');
    error.statusCode = 503;
    throw error;
  }

  const credentials = Buffer.from(
    env('SKLAD_YOOKASSA_SHOP_ID') + ':' + env('SKLAD_YOOKASSA_SECRET_KEY')
  ).toString('base64');

  const response = await fetch('https://api.yookassa.ru/v3' + path, {
    method: options.method || 'GET',
    headers: {
      'Authorization': 'Basic ' + credentials,
      'Accept': 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(options.idempotenceKey ? { 'Idempotence-Key': options.idempotenceKey } : {})
    },
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const text = await response.text();
  let parsed = {};
  try {
    parsed = text ? JSON.parse(text) : {};
  } catch {
    parsed = { raw: text };
  }

  if (!response.ok) {
    console.error('YooKassa request failed', response.status, path);
    const error = new Error('PAYMENT_PROVIDER_ERROR');
    error.statusCode = 502;
    error.providerStatus = response.status;
    throw error;
  }

  return parsed;
}

function buildReceipt(decoded, plan) {
  const vatCode = Number(env('SKLAD_RECEIPT_VAT_CODE'));
  if (!Number.isInteger(vatCode) || vatCode <= 0) return undefined;
  if (!decoded.email) return undefined;

  return {
    customer: { email: decoded.email },
    items: [
      {
        description: plan.title,
        quantity: '1.00',
        amount: {
          value: plan.amount,
          currency: plan.currency
        },
        vat_code: vatCode,
        payment_mode: 'full_payment',
        payment_subject: 'service'
      }
    ]
  };
}

async function createPayment(req, decoded, body) {
  const planId = String(body?.planId || '');
  const plan = plans()[planId];
  if (!plan) {
    const error = new Error('UNKNOWN_PLAN');
    error.statusCode = 400;
    throw error;
  }

  const suppliedKey = String(req.headers['idempotence-key'] || '').trim();
  const idempotenceKey =
    suppliedKey && suppliedKey.length <= 64 ? suppliedKey : crypto.randomUUID();

  const receipt = buildReceipt(decoded, plan);
  const payload = {
    amount: {
      value: plan.amount,
      currency: plan.currency
    },
    capture: true,
    confirmation: {
      type: 'redirect',
      return_url: env('SKLAD_PAYMENT_RETURN_URL') || 'skladpro://payment_success'
    },
    description: plan.title + ' — Склад ПРО',
    metadata: {
      app: 'sklad-pro',
      uid: decoded.uid,
      planId
    },
    ...(receipt ? { receipt } : {})
  };

  const payment = await yooRequest('/payments', {
    method: 'POST',
    idempotenceKey,
    body: payload
  });

  const paymentId = String(payment.id || '');
  if (!paymentId) {
    const error = new Error('PAYMENT_CREATE_INVALID_RESPONSE');
    error.statusCode = 502;
    throw error;
  }

  await getFirestore().collection('payments').doc(paymentId).set({
    paymentId,
    uid: decoded.uid,
    planId,
    expectedAmount: plan.amount,
    currency: plan.currency,
    status: String(payment.status || 'pending'),
    idempotenceKey,
    createdAt: Timestamp.now(),
    updatedAt: Timestamp.now(),
    grantApplied: false
  }, { merge: true });

  return {
    paymentId,
    status: payment.status || 'pending',
    confirmationUrl: payment.confirmation?.confirmation_url || ''
  };
}

async function validateAndApplyPayment(paymentId, expectedUid = '') {
  const db = getFirestore();
  const paymentRef = db.collection('payments').doc(paymentId);
  const storedSnap = await paymentRef.get();

  if (!storedSnap.exists) {
    const error = new Error('PAYMENT_NOT_FOUND');
    error.statusCode = 404;
    throw error;
  }

  const stored = storedSnap.data() || {};
  if (expectedUid && stored.uid !== expectedUid) {
    const error = new Error('PAYMENT_FORBIDDEN');
    error.statusCode = 403;
    throw error;
  }

  const remote = await yooRequest('/payments/' + encodeURIComponent(paymentId));
  const remoteUid = String(remote.metadata?.uid || '');
  const remotePlanId = String(remote.metadata?.planId || '');
  const plan = plans()[stored.planId];

  if (!plan || remotePlanId !== stored.planId || remoteUid !== stored.uid) {
    const error = new Error('PAYMENT_BINDING_MISMATCH');
    error.statusCode = 409;
    throw error;
  }

  const remoteAmount = normalizeMoney(remote.amount?.value);
  if (remote.amount?.currency !== plan.currency || remoteAmount !== plan.amount) {
    const error = new Error('PAYMENT_AMOUNT_MISMATCH');
    error.statusCode = 409;
    throw error;
  }

  await paymentRef.set({
    status: String(remote.status || stored.status || 'unknown'),
    updatedAt: Timestamp.now()
  }, { merge: true });

  if (remote.status !== 'succeeded') {
    return {
      paymentId,
      status: remote.status || 'pending',
      entitlement: await readEntitlement(stored.uid)
    };
  }

  const entitlement = await grantSubscription(paymentId, stored.uid, plan);
  return { paymentId, status: 'succeeded', entitlement };
}

async function grantSubscription(paymentId, uid, plan) {
  const db = getFirestore();
  const paymentRef = db.collection('payments').doc(paymentId);
  const entitlementRef = db.collection('entitlements').doc(uid);

  await db.runTransaction(async (tx) => {
    const [paymentSnap, entitlementSnap] = await Promise.all([
      tx.get(paymentRef),
      tx.get(entitlementRef)
    ]);

    const paymentData = paymentSnap.data() || {};
    if (paymentData.grantApplied === true) return;

    const entitlement = entitlementSnap.data() || {};
    const now = Date.now();
    const existingPaidUntil = timestampMillis(entitlement.paidUntil);
    const startsAt = Math.max(now, existingPaidUntil);
    const paidUntil = startsAt + plan.durationDays * 86400000;

    tx.set(entitlementRef, {
      uid,
      status: 'pro',
      planId: plan.id,
      paidUntil: Timestamp.fromMillis(paidUntil),
      updatedAt: Timestamp.fromMillis(now)
    }, { merge: true });

    tx.set(paymentRef, {
      status: 'succeeded',
      grantApplied: true,
      grantedAt: Timestamp.fromMillis(now),
      updatedAt: Timestamp.fromMillis(now)
    }, { merge: true });
  });

  return readEntitlement(uid);
}

async function handleWebhook(body) {
  const paymentId = String(body?.object?.id || '').trim();
  if (!paymentId) return { received: true };

  try {
    const result = await validateAndApplyPayment(paymentId);
    return { received: true, status: result.status };
  } catch (error) {
    if (error.message === 'PAYMENT_NOT_FOUND') {
      // A payment that was not created by this backend is intentionally ignored.
      return { received: true, ignored: true };
    }
    throw error;
  }
}

function publicPlans() {
  return Object.values(plans()).map((plan) => ({
    id: plan.id,
    title: plan.title,
    durationDays: plan.durationDays,
    amount: plan.amount,
    currency: plan.currency
  }));
}

const server = http.createServer(async (req, res) => {
  setCors(req, res);

  if (req.method === 'OPTIONS') {
    res.statusCode = 204;
    return res.end();
  }

  const url = new URL(req.url || '/', 'http://' + (req.headers.host || 'localhost'));
  const path = url.pathname;

  try {
    if (req.method === 'GET' && path === '/health') {
      return json(res, 200, {
        ok: true,
        service: 'sklad-pro-backend',
        firebaseConfigured: firebaseReady,
        paymentConfigured: paymentConfigured(),
        plansConfigured: publicPlans().length > 0,
        demoDays: DEMO_DAYS
      });
    }

    if (req.method === 'GET' && path === '/v1/plans') {
      return json(res, 200, {
        ok: true,
        plans: publicPlans()
      });
    }

    if (req.method === 'POST' && path === '/v1/bootstrap') {
      const decoded = await requireUser(req);
      const entitlement = await bootstrapUser(decoded);
      return json(res, 200, { ok: true, entitlement });
    }

    if (req.method === 'GET' && path === '/v1/me') {
      const decoded = await requireUser(req);
      return json(res, 200, {
        ok: true,
        uid: decoded.uid,
        email: decoded.email || '',
        entitlement: await readEntitlement(decoded.uid)
      });
    }

    if (req.method === 'POST' && path === '/v1/workspaces') {
      const decoded = await requireUser(req);
      const body = await readJson(req);
      const workspace = await createWorkspace(decoded, body);
      return json(res, 201, { ok: true, workspace });
    }

    if (req.method === 'POST' && path === '/v1/devices') {
      const decoded = await requireUser(req);
      const body = await readJson(req);
      const device = await registerDevice(decoded, body);
      return json(res, 200, { ok: true, device });
    }

    if (req.method === 'GET' && path === '/v1/devices') {
      const decoded = await requireUser(req);
      const devices = await listDevices(decoded);
      return json(res, 200, { ok: true, devices });
    }

    if (req.method === 'DELETE' && path.startsWith('/v1/devices/')) {
      const decoded = await requireUser(req);
      const installationId = decodeURIComponent(path.slice('/v1/devices/'.length));
      const result = await deleteDevice(decoded, installationId);
      return json(res, 200, { ok: true, ...result });
    }

    if (req.method === 'POST' && path === '/v1/payments') {
      const decoded = await requireUser(req);
      const body = await readJson(req);
      const result = await createPayment(req, decoded, body);
      return json(res, 201, { ok: true, ...result });
    }

    if (req.method === 'GET' && path === '/v1/payments/status') {
      const decoded = await requireUser(req);
      const paymentId = String(url.searchParams.get('payment_id') || '').trim();
      if (!paymentId) {
        const error = new Error('PAYMENT_ID_REQUIRED');
        error.statusCode = 400;
        throw error;
      }
      const result = await validateAndApplyPayment(paymentId, decoded.uid);
      return json(res, 200, { ok: true, ...result });
    }

    if (req.method === 'POST' && path === '/v1/webhooks/yookassa') {
      const body = await readJson(req);
      const result = await handleWebhook(body);
      return json(res, 200, { ok: true, ...result });
    }

    return json(res, 404, { ok: false, error: 'NOT_FOUND' });
  } catch (error) {
    const status = Number(error?.statusCode || 500);
    const safeMessage = status >= 500 ? 'INTERNAL_ERROR' : String(error?.message || 'REQUEST_FAILED');
    if (status >= 500) {
      console.error('Request failed', req.method, path, error?.message || error);
    }
    return json(res, status, {
      ok: false,
      error: safeMessage
    });
  }
});

if (require.main === module) {
  server.listen(PORT, '0.0.0.0', () => {
    console.log('Sklad PRO backend listening on port ' + PORT);
  });
}

module.exports = {
  effectiveEntitlement,
  normalizeMoney,
  timestampMillis,
  requireVerifiedEmail,
  VALID_PROFILE_IDS,
  normalizeDevicePayload,
  EXPECTED_FIREBASE_PROJECT_ID,
  isExpectedFirebaseProject
};
