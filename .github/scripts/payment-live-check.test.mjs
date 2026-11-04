import assert from 'node:assert/strict';
import { checkPaymentApi } from './payment-live-check.mjs';

const health = { ok: true, service: 'kapterka-payment-api', apiVersion: 2, secretConfigured: true, licenseRegistryConfigured: true, emailConfigured: true, fulfillmentVersion: 1 };
const gateway = { ok: true, paid: false, status: 'not_found' };
const mail = { ok: true, relay_url_valid: true, secret_matches: true, post_wrong_secret: { error: 'FORBIDDEN' }, post_real_secret: { error: 'INVALID_EMAIL' } };
function fake(overrides = {}) {
  const actions = [];
  const fetchImpl = async (url, init) => {
    const action = new URL(url).searchParams.get('action');
    actions.push(action);
    assert.ok(['health', 'check', 'mail_diag'].includes(action), 'diagnostic must never create payments or send messages');
    const entry = overrides[action] || { health, check: gateway, mail_diag: mail }[action];
    return new Response(JSON.stringify(entry.body || entry), { status: Number.isInteger(entry.status) ? entry.status : 200, headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': 'https://kapterka-pro.ru' } });
  };
  return { fetchImpl, actions };
}

const good = fake();
const result = await checkPaymentApi({ fetchImpl: good.fetchImpl, requireFulfillment: true });
assert.equal(result.ok, true);
assert.equal(result.realChargeVerified, false);
assert.equal(result.emailSent, false);
assert.deepEqual(good.actions, ['health', 'check', 'mail_diag']);
await assert.rejects(checkPaymentApi({ fetchImpl: fake({ check: { status: 502, body: { ok: false, error: 'PAYMENT_GATEWAY_AUTH' } } }).fetchImpl }), /PAYMENT_GATEWAY_AUTH/);
await assert.rejects(checkPaymentApi({ fetchImpl: fake({ check: { ok: true, paid: true, status: 'succeeded' } }).fetchImpl }), /Unexpected gateway/);
await assert.rejects(checkPaymentApi({ fetchImpl: fake({ mail_diag: { ...mail, secret_matches: false } }).fetchImpl }), /Mail relay access/);
await assert.rejects(checkPaymentApi({ fetchImpl: fake({ health: { ...health, fulfillmentVersion: 0 } }).fetchImpl, requireFulfillment: true }), /not deployed/);
console.log('5/5 read-only payment diagnostic checks passed');
