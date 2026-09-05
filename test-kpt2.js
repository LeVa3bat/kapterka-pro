const CHECKSUM_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
function computeKeyChecksumJS(p1, p2, prefix) {
  const s = `${prefix}-${p1}-${p2}-KAPT3RKA_881_MILITARY`;
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
console.log("KAPT", computeKeyChecksumJS("07EF", "03D1", "KAPT"));
console.log("KPT", computeKeyChecksumJS("07EF", "03D1", "KPT"));
