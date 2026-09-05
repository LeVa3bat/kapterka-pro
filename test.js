const CHECKSUM_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
function computeKeyChecksumJS(p1, p2) {
  const s = `KAPT-${p1}-${p2}-KAPT3RKA_881_MILITARY`;
  let h1 = 0x811c9dc5 >>> 0;
  let h2 = 0x5a2d1e39 >>> 0;
  for (let i = 0; i < s.length; i++) {
    const code = s.charCodeAt(i);
    h1 = Math.imul(h1 ^ code, 0x01000193) >>> 0;
    h2 = (Math.imul(h2 + code, 31) + 0x45) >>> 0;
  }
  const c0 = CHECKSUM_CHARS[(h1 >>> 24) & 0x1F];
  const c1 = CHECKSUM_CHARS[(h1 >>> 16) & 0x1F];
  const c2 = CHECKSUM_CHARS[(h2 >>> 24) & 0x1F];
  const c3 = CHECKSUM_CHARS[(h2 >>> 16) & 0x1F];
  return `${c0}${c1}${c2}${c3}`;
}
function computeKeyChecksumKotlinEquivalent(p1, p2) {
  const s = `KAPT-${p1}-${p2}-KAPT3RKA_881_MILITARY`;
  let h1 = 0x811c9dc5n;
  let h2 = 0x5a2d1e39n;
  for (let i = 0; i < s.length; i++) {
    const code = BigInt(s.charCodeAt(i));
    h1 = ((h1 ^ code) * 0x01000193n) & 0xFFFFFFFFn;
    h2 = (((h2 + code) * 31n) + 0x45n) & 0xFFFFFFFFn;
  }
  const c0 = CHECKSUM_CHARS[Number((h1 >> 24n) & 0x1Fn)];
  const c1 = CHECKSUM_CHARS[Number((h1 >> 16n) & 0x1Fn)];
  const c2 = CHECKSUM_CHARS[Number((h2 >> 24n) & 0x1Fn)];
  const c3 = CHECKSUM_CHARS[Number((h2 >> 16n) & 0x1Fn)];
  return `${c0}${c1}${c2}${c3}`;
}

console.log("JS: ", computeKeyChecksumJS("ABCD", "EFGH"));
console.log("Kotlin Equivalent: ", computeKeyChecksumKotlinEquivalent("ABCD", "EFGH"));
