# Android next-safe audit — 2026-09-22

## Baseline that must not be broken

- Stable app: 3.4.9 / versionCode 31
- Package: com.aistudio.kapterka.jmwqve
- minSdk: 24 (Android 7.0+)
- Room database: kapterka_database, schema version 2
- Published signer SHA-256 baseline: 843a7e883914f3a7a5a7665ff07b2e8c43da87a24ee4dc35e1600758aee73cb9
- Working web release remains independent from this branch.

## Critical findings

### P0 — YooKassa secret is embedded in Android source
The Android client currently contains live YooKassa credential material and calls YooKassa API directly.
Because the repository is public and secrets embedded in APKs are extractable, this must be migrated to a server-only payment API before a future security release.

Do not revoke/rotate the currently used YooKassa credential until the server path is deployed and the current production payment path is understood, otherwise payment in 3.4.9 can break.

Target design:
1. Android sends only callsign/email/return URL to our payment backend.
2. Backend stores YooKassa secret only in environment variables.
3. Backend creates payments and verifies status with YooKassa.
4. Android never receives or stores shop secret.
5. Credential rotation happens after the new path is deployed and verified.

### P0 — License activation can be forged locally
The license checksum algorithm and its seed are present in the Android client.
There is also compatibility handling for KPT-format keys and an offline fallback that can activate a locally valid key even when Firestore cannot confirm it.

Target design:
- Server/Firestore registry is authoritative for paid licenses.
- Offline mode may continue an already-confirmed license until its stored expiry, but must not mint a fresh 30-day license from an unregistered key.
- Remove unconditional KPT bypass after checking whether any real users still depend on those legacy keys.
- Preserve existing valid paid users during migration.

### P0 — Sync reconciliation can delete local data
Current sync uses cloud-authoritative reconciliation. When cloud collections do not contain a local stock/operation/point, local rows may be deleted. An empty or stale cloud can therefore erase local data during reconcile.

Target design:
- Never infer destructive deletion from absence alone.
- Introduce explicit deletion/tombstone metadata or another conflict-safe rule.
- Before any destructive reconciliation, create a recoverable local snapshot.
- Add tests for: first sync, empty cloud, stale cloud, two-device edit, delete on one device, offline edits, reconnect.

### P0 — Exact signing private key must be recovered before an update-compatible APK can be built
The APK contained in the user-provided project archive was inspected locally. Its signer certificate SHA-256 matches the published baseline exactly:
`843a7e883914f3a7a5a7665ff07b2e8c43da87a24ee4dc35e1600758aee73cb9`.

The certificate subject is Android Debug, confirming that existing installs depend on that signing identity. However, the corresponding private `debug.keystore` / JKS file is not present in the uploaded project archive or tracked repository.

This is a hard release gate:
- development and source refactoring may continue safely in this branch;
- do not claim an APK is update-compatible until it is signed with the exact matching private key;
- recover the original keystore from the build machine / secure backup before release testing;
- never replace it with a newly generated debug key.

### P1 — Release signing is intentionally pinned to the published baseline
Gradle release currently uses debugConfig. This looks unusual, but the repository invariant explicitly protects it because the published 3.4.9 signer must remain identical.

Do not switch signing configs until the actual keystore producing the published signer is available and verified. A signing change would prevent installation over existing users.

### P1 — Backup rules are still default/sample
Android backup/data extraction rules are effectively unconfigured. The sync device UUID is stored in a dedicated SharedPreferences file and should not be restored to a second phone as the same physical device identity.

Target design:
- Preserve Room data and user/license data as intended.
- Exclude ephemeral device identity from device/cloud restore.
- Test Android device transfer and restore behavior before release.

### P1 — Privileged notification/admin actions originate from the client
Android can call a public notification endpoint, and developer/admin code can write license/fighter records directly to Firestore. Email provider credentials can also be stored in client SharedPreferences if configured.

Target design:
- Payment/license issuance and administrative license grants must be server-authoritative.
- Notification endpoints must require server-side authorization/rate limiting and must not accept arbitrary privileged messages from any unauthenticated client.
- Email provider/API credentials must stay on the server, not in the APK or Android SharedPreferences.
- Firestore rules must prevent an ordinary client from minting/changing licenses or reading global fighter records.

### P1 — Local operation + stock updates are not a single Room transaction
Repository operations write history and stock in several DAO calls. A process death between calls can leave history and balances inconsistent.

Target design:
- Group each business operation and all affected stock updates inside one Room transaction.
- Sync only after local transaction commits.
- Add rollback/consistency tests.


## Implemented in android/next-safe

### Payment client hardening
- Android no longer embeds the YooKassa secret in `YooKassaPaymentService`.
- Direct authenticated calls from the APK to YooKassa were removed from the future branch.
- The future client now accepts only a public `PAYMENT_API_URL` and delegates payment creation/status checks to the backend.
- Legacy `saveConfig(...)` remains source-compatible but deliberately ignores/removes secret material instead of persisting it.
- `.env.example` now documents which settings belong on Android vs server only.

### Sync data-loss guard
- Initial reconcile no longer deletes local warehouse points, stock, operation history or requisitions merely because they are absent from the cloud snapshot.
- An empty cloud stock collection no longer clears local stock.
- This is a conservative safety guard. Explicit deletion/tombstone semantics still need to be designed before release.

## Safe implementation order

1. Establish green baseline compile + unit tests on this branch.
2. Payment backend/client split without changing production 3.4.9.
3. License verification migration with backward compatibility.
4. Sync data-loss protection and conflict tests.
5. Backup/device-transfer rules.
6. Room transactional operations.
7. UI/stability/performance improvements.
8. Only then choose next versionName/versionCode and build a test APK.
9. Verify test APK installs over 3.4.9 and preserves data, license, device count and sync.
10. Never merge/release until all safety gates pass.

## Explicit non-goals for the audit stage

- Do not change package name.
- Do not change Room filename.
- Do not use destructive migration.
- Do not change published signing identity.
- Do not publish APK from this branch.
- Do not modify current RuStore release while moderation is active.
