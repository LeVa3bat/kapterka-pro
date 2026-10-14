#!/usr/bin/env node
// Read-only APK validation for a future catalog submission. No upload, login,
// analytics or secret access. JSON goes to stdout; the caller may save it.
import { createHash } from 'node:crypto';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { basename, dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const pinnedPackage = 'com.aistudio.kapterka.jmwqve';
const pinnedSigner = '843a7e883914f3a7a5a7665ff07b2e8c43da87a24ee4dc35e1600758aee73cb9';
const flags = { apk: join(root, 'docs/kapterka-pro.apk'), release: join(root, 'docs/release.json'), public: false };
const args = process.argv.slice(2);
for (let i = 0; i < args.length; i++) {
  const arg = args[i];
  if (arg === '--public') flags.public = true;
  else if (['--apk', '--release', '--build-tools'].includes(arg) && args[i + 1] && !args[i + 1].startsWith('--')) flags[arg.slice(2)] = resolve(args[++i]);
  else {
    process.stderr.write('Usage: node tools/growth/catalog-apk-check.mjs [--apk PATH] [--release PATH] [--build-tools DIRECTORY] [--public]\n');
    process.exit(2);
  }
}

const report = {
  checkedAtUtc: new Date().toISOString(),
  scope: 'Read-only verification; no store submission or publication',
  apkPath: flags.apk,
  releaseManifestPath: flags.release,
  checks: [],
  submissionPerformed: false,
  published: false,
  physicalDeviceTestPerformed: false,
  actualApkScreenshotsCaptured: false,
  malwareScanResult: null
};
function check(name, passed, observed, expected) {
  report.checks.push({ name, passed: Boolean(passed), observed, ...(expected === undefined ? {} : { expected }) });
}
function digest(bytes) { return createHash('sha256').update(bytes).digest('hex'); }
function command(executable, commandArgs) {
  const result = spawnSync(executable, commandArgs, { cwd: dirname(flags.apk), encoding: 'utf8', timeout: 20000, windowsHide: true, maxBuffer: 4 * 1024 * 1024 });
  if (result.error || result.status !== 0) throw new Error(`${executable} failed (${result.error?.code ?? result.status}); APK was not validated`);
  return `${result.stdout ?? ''}\n${result.stderr ?? ''}`.replace(/\r\n/g, '\n');
}
function findBuildTools() {
  if (flags['build-tools']) return flags['build-tools'];
  // Read only these conventional tool-path variables, never credentials.
  const sdkRoots = [process.env.ANDROID_SDK_ROOT, process.env.ANDROID_HOME, process.platform === 'win32' ? 'C:/Android/Sdk' : undefined].filter(Boolean);
  for (const sdkRoot of sdkRoots) {
    const dir = join(sdkRoot, 'build-tools');
    if (!existsSync(dir)) continue;
    const candidates = readdirSync(dir, { withFileTypes: true }).filter(entry => entry.isDirectory()).map(entry => entry.name).sort((a, b) => b.localeCompare(a, undefined, { numeric: true }));
    for (const candidate of candidates) {
      const path = join(dir, candidate);
      if (existsSync(join(path, process.platform === 'win32' ? 'aapt.exe' : 'aapt')) && existsSync(join(path, 'lib/apksigner.jar'))) return path;
    }
  }
  throw new Error('Android build-tools not found; use --build-tools DIRECTORY. Signature verification must not be skipped.');
}
async function getPublic(url) {
  const response = await fetch(url, { signal: AbortSignal.timeout(20000), redirect: 'follow' });
  if (!response.ok) throw new Error(`Public source HTTP ${response.status}: ${url}`);
  return { response, bytes: Buffer.from(await response.arrayBuffer()) };
}

try {
  const release = JSON.parse(readFileSync(flags.release, 'utf8'));
  const apk = readFileSync(flags.apk);
  const apkHash = digest(apk);
  report.apk = { sha256: apkHash, sizeBytes: apk.length };
  check('APK SHA-256 matches release.json', apkHash === release.apkSha256, apkHash, release.apkSha256);
  check('APK size matches release.json', apk.length === release.apkSize, apk.length, release.apkSize);
  const buildTools = findBuildTools();
  const aapt = join(buildTools, process.platform === 'win32' ? 'aapt.exe' : 'aapt');
  // aapt on Windows cannot always parse non-ASCII absolute argument paths.
  // Run from the APK directory and pass only its filename; never copy the APK.
  const badging = command(aapt, ['dump', 'badging', basename(flags.apk)]);
  const packageMatch = badging.match(/^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'/m);
  if (!packageMatch) throw new Error('aapt did not identify the Android package/version');
  const metadata = {
    packageName: packageMatch[1],
    versionCode: Number(packageMatch[2]),
    versionName: packageMatch[3],
    minSdk: Number(badging.match(/^sdkVersion:'(\d+)'/m)?.[1]),
    targetSdk: Number(badging.match(/^targetSdkVersion:'(\d+)'/m)?.[1]),
    nativeAbis: [...(badging.match(/^native-code:(.+)$/m)?.[1] ?? '').matchAll(/'([^']+)'/g)].map(match => match[1])
  };
  Object.assign(report.apk, metadata);
  check('Package identity preserved', metadata.packageName === pinnedPackage && metadata.packageName === release.packageName, metadata.packageName, pinnedPackage);
  check('Version name matches release.json', metadata.versionName === release.versionName, metadata.versionName, release.versionName);
  check('Version code matches release.json', metadata.versionCode === release.versionCode, metadata.versionCode, release.versionCode);
  check('Minimum SDK matches release.json', metadata.minSdk === release.minSdk, metadata.minSdk, release.minSdk);
  const manifest = command(aapt, ['dump', 'xmltree', basename(flags.apk), 'AndroidManifest.xml']);
  check('APK is not marked debuggable/testOnly', !/android:(?:debuggable|testOnly)\([^\n]*=\(type 0x12\)0xffffffff/i.test(manifest), /android:(?:debuggable|testOnly)\([^\n]*=\(type 0x12\)0xffffffff/i.test(manifest) ? 'true' : 'false or absent');
  // java -jar avoids invoking a .bat or interpreting user paths in a shell.
  const signing = command('java', ['-jar', join(buildTools, 'lib/apksigner.jar'), 'verify', '--verbose', '--print-certs', flags.apk]);
  const signer = signing.match(/^Signer #1 certificate SHA-256 digest: ([0-9a-f]+)$/mi)?.[1]?.toLowerCase();
  const signerCount = Number(signing.match(/^Number of signers: (\d+)$/m)?.[1]);
  Object.assign(report.apk, {
    signerSha256: signer,
    signerDn: signing.match(/^Signer #1 certificate DN: (.+)$/m)?.[1],
    signatureSchemes: [...signing.matchAll(/^Verified using (v[^ ]+) scheme[^:]*: true$/gm)].map(match => match[1])
  });
  check('Cryptographic signature verifies', /^Verifies$/m.test(signing), /^Verifies$/m.test(signing));
  check('Single historical signer preserved', signerCount === 1 && signer === pinnedSigner && signer === release.signerSha256, { signerCount, signerSha256: signer }, { signerCount: 1, signerSha256: pinnedSigner });
  report.tools = { buildTools, aapt, apksignerJar: join(buildTools, 'lib/apksigner.jar') };

  if (flags.public) {
    const [download, publicRelease] = await Promise.all([getPublic('https://kapterka-pro.ru/kapterka-pro.apk'), getPublic('https://kapterka-pro.ru/release.json')]);
    const publicHash = digest(download.bytes);
    const publicMetadata = JSON.parse(publicRelease.bytes.toString('utf8'));
    report.publicSource = { apkUrl: download.response.url, httpStatus: download.response.status, contentType: download.response.headers.get('content-type'), sha256: publicHash, sizeBytes: download.bytes.length, releaseManifestUrl: publicRelease.response.url };
    check('Live APK equals verified local APK', publicHash === apkHash && download.bytes.length === apk.length, { sha256: publicHash, sizeBytes: download.bytes.length }, { sha256: apkHash, sizeBytes: apk.length });
    check('Live release manifest matches candidate', ['versionName', 'versionCode', 'packageName', 'minSdk', 'apkSize', 'apkSha256', 'signerSha256'].every(key => publicMetadata[key] === release[key]), publicMetadata.versionName, release.versionName);
    check('Live APK has Android MIME type', /application\/vnd\.android\.package-archive/i.test(download.response.headers.get('content-type') ?? ''), download.response.headers.get('content-type'));
  }
} catch (error) {
  report.error = error.message;
}
report.passed = !report.error && report.checks.length > 0 && report.checks.every(item => item.passed);
process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
if (!report.passed) process.exitCode = 1;
