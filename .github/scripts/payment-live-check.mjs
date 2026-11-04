// Read-only production checks: no payment creation, licence grant or email send.
import { pathToFileURL } from 'node:url';

export const DEFAULT_PAYMENT_API = 'https://kapterka-api.alex-666-881.workers.dev';

export async function checkPaymentApi({ fetchImpl = fetch, baseUrl = DEFAULT_PAYMENT_API, requireFulfillment = false } = {}) {
  const request = async (action, body) => {
    const response = await fetchImpl(`${baseUrl}/?action=${action}`, {
      method: body ? 'POST' : 'GET',
      headers: { 'Content-Type': 'application/json', Origin: 'https://kapterka-pro.ru' },
      ...(body ? { body: JSON.stringify(body) } : {}),
      signal: AbortSignal.timeout(action === 'mail_diag' ? 65000 : 25000)
    });
    const data = await response.json().catch(() => null);
    if (!response.ok || !data || data.ok !== true) {
      throw new Error(`${action}: HTTP ${response.status}, ${data?.error || 'invalid response'}`);
    }
    if (response.headers.get('Access-Control-Allow-Origin') !== 'https://kapterka-pro.ru') {
      throw new Error(`${action}: official website CORS is missing`);
    }
    return data;
  };

  const health = await request('health');
  if (health.service !== 'kapterka-payment-api' || health.apiVersion !== 2) throw new Error('Unexpected payment API');
  for (const field of ['secretConfigured', 'licenseRegistryConfigured', 'emailConfigured']) {
    if (health[field] !== true) throw new Error(`Missing configuration: ${field}`);
  }
  if (requireFulfillment && health.fulfillmentVersion !== 1) throw new Error('Automatic licence fulfilment is not deployed');

  // This fixed nonexistent ID verifies shop authentication without creating a
  // transaction. The deliberately different fighter also prevents any grant.
  const gateway = await request('check', {
    payment_id: '2f000000-000f-5000-8000-000000000000', fighter_id: 'HEALTHCHECK'
  });
  if (gateway.paid !== false || gateway.status !== 'not_found') throw new Error('Unexpected gateway diagnostic response');

  // mail_diag uses an invalid destination; neither relay request sends a letter.
  const mail = await request('mail_diag', {});
  if (mail.relay_url_valid) {
    if (mail.secret_matches !== true || mail.post_wrong_secret?.error !== 'FORBIDDEN' || mail.post_real_secret?.error !== 'INVALID_EMAIL') {
      throw new Error('Mail relay access diagnostic failed');
    }
  } else if (mail.brevo !== true) throw new Error('No email provider configured');

  return {
    ok: true, gateway: 'authenticated', registry: 'configured',
    email: mail.relay_url_valid ? 'relay_access_verified_without_sending' : 'provider_configured_only',
    fulfillmentVersion: health.fulfillmentVersion || 0,
    realChargeVerified: false, licenceIssued: false, emailSent: false
  };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  checkPaymentApi({ requireFulfillment: process.argv.includes('--require-fulfillment') })
    .then((result) => console.log(JSON.stringify(result, null, 2)))
    .catch((error) => { console.error(`Payment API check FAILED: ${error.message}`); process.exitCode = 1; });
}
