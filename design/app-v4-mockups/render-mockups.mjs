/**
 * Generates pixel-exact Android app v4.0 UI mockups (Path A) for owner approval
 * before rewriting Jetpack Compose screens.
 *
 * Renders 8 full 412x915 dp screens at 2.5x DPR (= 1030x2288 px crisp PNGs) plus
 * a 4x2 overview board (00-overview-board.png) and enforces strict layout guards:
 * - zero horizontal overflow
 * - zero unintended text wrapping on buttons, tabs, chips, badges, nav labels
 * - exact equal heights across sibling action buttons
 */
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require('playwright');

const root = path.resolve(path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')), '../..');
const outDir = path.join(root, 'design/app-v4-mockups');
const fontsDir = path.join(root, 'docs/fonts');

function fontDataUrl(file) {
  return 'data:font/woff2;base64,' + fs.readFileSync(path.join(fontsDir, file)).toString('base64');
}

const FONT_CSS = `
@font-face{font-family:"IBM Plex Sans";font-weight:400;src:url("${fontDataUrl('plex-sans-400-cyrillic.woff2')}") format("woff2");unicode-range:U+0301,U+0400-045F,U+0490-0491,U+04B0-04B1,U+2116}
@font-face{font-family:"IBM Plex Sans";font-weight:400;src:url("${fontDataUrl('plex-sans-400-latin.woff2')}") format("woff2");unicode-range:U+0000-00FF}
@font-face{font-family:"IBM Plex Sans";font-weight:600;src:url("${fontDataUrl('plex-sans-600-cyrillic.woff2')}") format("woff2");unicode-range:U+0301,U+0400-045F,U+0490-0491,U+04B0-04B1,U+2116}
@font-face{font-family:"IBM Plex Sans";font-weight:600;src:url("${fontDataUrl('plex-sans-600-latin.woff2')}") format("woff2");unicode-range:U+0000-00FF}
@font-face{font-family:"IBM Plex Mono";font-weight:500;src:url("${fontDataUrl('plex-mono-500-cyrillic.woff2')}") format("woff2");unicode-range:U+0301,U+0400-045F,U+0490-0491,U+04B0-04B1,U+2116}
@font-face{font-family:"IBM Plex Mono";font-weight:500;src:url("${fontDataUrl('plex-mono-500-latin.woff2')}") format("woff2");unicode-range:U+0000-00FF}
`;

// Crisp 1.8px stroke inline SVG icons
const IC = {
  sync: '<svg viewBox="0 0 24 24"><path d="M21 12a9 9 0 0 1-15.4 6.4L3 16m0-4a9 9 0 0 1 15.4-6.4L21 8"/><path d="M3 21v-5h5M21 3v5h-5"/></svg>',
  qr: '<svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><path d="M14 14h3v3h-3zM18 18h3v3h-3zM14 20h2"/></svg>',
  excel: '<svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M8 13h8M8 17h6"/></svg>',
  help: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M9.5 9.5a2.5 2.5 0 1 1 4 2c-.9.6-1.5 1.1-1.5 2.2M12 17h.01"/></svg>',
  in: '<svg viewBox="0 0 24 24"><path d="M12 3v12m0 0-4-4m4 4 4-4M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2"/></svg>',
  out: '<svg viewBox="0 0 24 24"><path d="M12 15V3m0 0-4 4m4-4 4 4M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2"/></svg>',
  move: '<svg viewBox="0 0 24 24"><path d="M7 7h12m0 0-3.5-3.5M19 7l-3.5 3.5M17 17H5m0 0 3.5-3.5M5 17l3.5 3.5"/></svg>',
  burn: '<svg viewBox="0 0 24 24"><path d="M3 6h18M8 6V4h8v2m-9 0 1 14h8l1-14M10 11v5M14 11v5"/></svg>',
  search: '<svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>',
  chevDown: '<svg viewBox="0 0 24 24"><path d="m6 9 6 6 6-6"/></svg>',
  chevRight: '<svg viewBox="0 0 24 24"><path d="m9 6 6 6-6 6"/></svg>',
  box: '<svg viewBox="0 0 24 24"><path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3z"/><path d="M12 12 4 7.5M12 12l8-4.5M12 12v9"/></svg>',
  home: '<svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="8" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="15" width="7" height="6" rx="1.5"/></svg>',
  hist: '<svg viewBox="0 0 24 24"><path d="M4 5h16M4 12h16M4 19h10"/><circle cx="18.5" cy="19" r="2.5"/></svg>',
  req: '<svg viewBox="0 0 24 24"><rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 8h6M9 12h6M9 16h4"/></svg>',
  cat: '<svg viewBox="0 0 24 24"><path d="M4 7h16M4 12h16M4 17h16"/><circle cx="7" cy="7" r="1"/><circle cx="7" cy="12" r="1"/><circle cx="7" cy="17" r="1"/></svg>',
  more: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9c.26.6.85 1 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>',
  mail: '<svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m4 7 8 6 8-6"/></svg>',
  shield: '<svg viewBox="0 0 24 24"><path d="M12 3 4 6v6c0 4.5 3.3 7.8 8 9 4.7-1.2 8-4.5 8-9V6l-8-3z"/><path d="m9 12 2 2 4-4"/></svg>',
  wrench: '<svg viewBox="0 0 24 24"><path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L4 17l3 3 5.3-5.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2-2 2.5-2.5z"/></svg>',
  blank: '<svg viewBox="0 0 24 24"><rect x="4" y="4" width="16" height="16" rx="3"/><path d="M12 8v8M8 12h8"/></svg>',
  plus: '<svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>',
  wifiOff: '<svg viewBox="0 0 24 24"><path d="M3 3l18 18M8.5 16.5a5 5 0 0 1 7 0M5 12.5a10 10 0 0 1 5.2-2.6M15.4 10.2A10 10 0 0 1 19 12.5M12 20h.01"/></svg>',
  crown: '<svg viewBox="0 0 24 24"><path d="m3 18 2-10 4.5 4L12 5l2.5 7L19 8l2 10H3z"/></svg>',
  check: '<svg viewBox="0 0 24 24"><path d="m5 12 4.5 4.5L19 7"/></svg>'
};

const BASE_CSS = `
${FONT_CSS}
:root{
  --bg:#0B1115;
  --surf:#121B22;
  --surf2:#18242D;
  --surf3:#1E2C37;
  --line:rgba(148,179,191,.16);
  --line-s:rgba(52,211,153,.35);
  --em:#10B981;
  --em-b:#34D399;
  --em-bg:rgba(16,185,129,.14);
  --gold:#F59E0B;
  --gold-b:#FBBF24;
  --gold-bg:rgba(245,158,11,.14);
  --sky:#38BDF8;
  --sky-bg:rgba(56,189,248,.14);
  --red:#F87171;
  --red-bg:rgba(248,113,113,.14);
  --txt:#F1F6F8;
  --txt2:#B6C7D1;
  --muted:#7F95A3;
  --sans:"IBM Plex Sans",system-ui,sans-serif;
  --mono:"IBM Plex Mono",monospace;
}
*{box-sizing:border-box;margin:0;padding:0}
html,body{width:412px;height:915px;overflow:hidden;background:var(--bg);color:var(--txt);font-family:var(--sans);-webkit-font-smoothing:antialiased}
svg{width:18px;height:18px;stroke:currentColor;stroke-width:1.9;fill:none;stroke-linecap:round;stroke-linejoin:round;flex:none}
.phone{width:412px;height:915px;display:flex;flex-direction:column;background:radial-gradient(ellipse 90% 35% at 50% 0%,rgba(16,185,129,.12),transparent 60%),var(--bg);position:relative;overflow:hidden}
.statusbar{height:34px;padding:0 20px;display:flex;align-items:center;justify-content:space-between;font:600 12px/1 var(--mono);color:var(--txt2);flex:none}
.sb-icons{display:flex;align-items:center;gap:6px;font-size:11px}
.body{flex:1;min-height:0;overflow:hidden;padding:6px 16px 12px;display:flex;flex-direction:column;gap:10px}
.nav-wrap{flex:none;padding:6px 12px 14px;background:linear-gradient(to top,var(--bg) 70%,transparent)}
.nav-pill{height:62px;border-radius:20px;background:var(--surf);border:1px solid var(--line);display:grid;grid-template-columns:repeat(5,minmax(0,1fr));padding:5px;gap:2px;box-shadow:0 12px 28px rgba(0,0,0,.45)}
.nav-item{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;border-radius:15px;color:var(--muted);font:600 10.5px/1 var(--sans);white-space:nowrap}
.nav-item svg{width:20px;height:20px}
.nav-item.active{background:linear-gradient(180deg,rgba(16,185,129,.24),rgba(16,185,129,.09));color:var(--em-b)}
.nav-badge{position:relative}
.nav-badge i{position:absolute;top:-5px;right:-9px;min-width:16px;height:16px;padding:0 4px;border-radius:999px;background:var(--gold);color:#140D02;font:700 9.5px/16px var(--mono);font-style:normal;text-align:center}
.gesture{position:absolute;bottom:5px;left:50%;transform:translateX(-50%);width:116px;height:4px;border-radius:99px;background:rgba(241,246,248,.22)}

/* Header */
.top-head{display:flex;align-items:center;justify-content:space-between;gap:10px}
.top-title-wrap{min-width:0;flex:1}
.top-title{font:600 19px/1.15 var(--sans);letter-spacing:-.02em;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.top-sub{margin-top:2px;font:500 11px/1.2 var(--mono);color:var(--muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.prof-pill{display:flex;align-items:center;gap:8px;padding:5px 10px 5px 11px;border-radius:999px;background:var(--surf);border:1px solid var(--line);flex:none}
.prof-pill .p-name{font:600 11.5px/1.1 var(--sans);color:var(--txt);text-align:right;white-space:nowrap}
.prof-pill .p-sub{font:600 9.5px/1.15 var(--mono);color:var(--em-b);text-align:right;white-space:nowrap}
.prof-pill .p-sub.gold{color:var(--gold-b)}
.avatar{width:28px;height:28px;border-radius:50%;background:var(--em-bg);border:1px solid var(--line-s);color:var(--em-b);display:grid;place-items:center;font:600 12px/1 var(--sans);flex:none}

/* Equal 4-tile strips */
.grid-4{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px}
.svc-tile{height:54px;border-radius:13px;background:var(--surf);border:1px solid var(--line);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;position:relative;padding:0 4px}
.svc-tile svg{width:17px;height:17px;color:var(--em-b)}
.svc-tile .lbl{font:600 10.5px/1 var(--sans);color:var(--txt2);white-space:nowrap}
.svc-tile .sub{font:500 9px/1 var(--mono);color:var(--em-b);white-space:nowrap}
.svc-tile .dot{position:absolute;top:7px;right:7px;width:6px;height:6px;border-radius:50%;background:var(--em-b);box-shadow:0 0 6px var(--em-b)}

.op-btn{height:48px;border-radius:12px;border:1px solid transparent;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;padding:0 4px}
.op-btn svg{width:16px;height:16px}
.op-btn span{font:600 10.5px/1 var(--sans);white-space:nowrap}
.op-in{background:var(--em-bg);border-color:rgba(52,211,153,.32);color:var(--em-b)}
.op-out{background:var(--gold-bg);border-color:rgba(251,191,36,.32);color:var(--gold-b)}
.op-move{background:var(--sky-bg);border-color:rgba(56,189,248,.32);color:var(--sky)}
.op-burn{background:var(--red-bg);border-color:rgba(248,113,113,.3);color:var(--red)}

/* Search & chips */
.search-bar{height:40px;border-radius:12px;background:var(--surf);border:1px solid var(--line);padding:0 12px;display:flex;align-items:center;gap:8px;color:var(--muted);font-size:12.5px}
.search-bar svg{width:16px;height:16px}
.chips{display:flex;gap:6px;overflow:hidden}
.chip{height:30px;padding:0 11px;border-radius:999px;background:var(--surf);border:1px solid var(--line);color:var(--txt2);font:600 11px/30px var(--sans);white-space:nowrap;flex:none}
.chip.active{background:var(--em);border-color:var(--em);color:#04150E}

/* Collapsible Accordion Cards */
.acc-list{display:flex;flex-direction:column;gap:8px;flex:1;min-height:0}
.acc{border-radius:16px;background:var(--surf);border:1px solid var(--line);overflow:hidden}
.acc.open{border-color:rgba(52,211,153,.34)}
.acc-head{height:50px;padding:0 12px;display:flex;align-items:center;justify-content:space-between;gap:8px}
.acc-left{display:flex;align-items:center;gap:9px;min-width:0;flex:1}
.acc-ico{width:30px;height:30px;border-radius:9px;background:var(--em-bg);color:var(--em-b);display:grid;place-items:center;flex:none}
.acc-ico.muted{background:var(--surf2);color:var(--txt2)}
.acc-ico svg{width:16px;height:16px}
.acc-name{font:600 13.5px/1.15 var(--sans);color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.acc-meta{font:500 10.5px/1.15 var(--mono);color:var(--muted);white-space:nowrap}
.acc-right{display:flex;align-items:center;gap:8px;flex:none}
.pill-num{height:24px;padding:0 8px;border-radius:999px;background:var(--surf2);border:1px solid var(--line);font:600 11px/22px var(--mono);color:var(--txt);white-space:nowrap}
.acc.open .pill-num{background:var(--em-bg);border-color:var(--line-s);color:var(--em-b)}
.acc-chev{color:var(--muted);width:16px;height:16px}

/* Aligned table inside expanded accordion */
.tbl-head{display:grid;grid-template-columns:1fr 74px 44px;gap:6px;padding:6px 12px;background:rgba(255,255,255,.025);border-top:1px solid var(--line);border-bottom:1px solid var(--line);font:600 10px/1 var(--mono);color:var(--muted);text-transform:uppercase;letter-spacing:.04em}
.tbl-head span:nth-child(2){text-align:right}
.tbl-head span:nth-child(3){text-align:center}
.tbl-row{display:grid;grid-template-columns:1fr 74px 44px;gap:6px;align-items:center;padding:8px 12px;border-bottom:1px solid rgba(148,179,191,.1)}
.tbl-row:last-child{border-bottom:0}
.tbl-name{font:600 12.5px/1.2 var(--sans);color:var(--txt);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.tbl-cat{font:400 10.5px/1.2 var(--sans);color:var(--muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.tbl-qty{font:600 13px/1 var(--mono);color:#fff;text-align:right;white-space:nowrap;font-variant-numeric:tabular-nums}
.tbl-qty small{font:500 10px/1 var(--sans);color:var(--muted);margin-left:3px}
.tbl-adj{height:26px;width:44px;border-radius:8px;background:var(--surf2);border:1px solid var(--line);color:var(--em-b);font:600 11px/24px var(--mono);text-align:center;white-space:nowrap}

/* Compact Demo/Status Banner */
.mini-banner{height:38px;padding:0 12px;border-radius:11px;background:rgba(245,158,11,.12);border:1px solid rgba(251,191,36,.32);display:flex;align-items:center;justify-content:space-between;gap:8px;font:600 11.5px/1 var(--sans);color:var(--gold-b)}
.mini-banner span{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.mini-banner b{padding:4px 9px;border-radius:7px;background:var(--gold);color:#140D02;font:700 10.5px/1 var(--sans);white-space:nowrap;flex:none}

/* Primary/Secondary Full Buttons */
.btn-row-2{display:grid;grid-template-columns:1fr 1fr;gap:8px}
.btn{height:44px;border-radius:12px;display:flex;align-items:center;justify-content:center;gap:7px;font:600 13px/1 var(--sans);white-space:nowrap;padding:0 12px;border:1px solid transparent}
.btn-em{background:linear-gradient(135deg,#34D399,#10B981);color:#04150E}
.btn-gold{background:linear-gradient(135deg,#FBBF24,#F59E0B);color:#160E02}
.btn-ghost{background:var(--surf2);border-color:var(--line);color:var(--txt2)}
.btn-sm{height:34px;border-radius:9px;font-size:11.5px;padding:0 10px}
.btn-compact{height:32px;padding:0 12px;border-radius:10px;font-size:11.5px}

/* Auth & Onboarding Specific */
.seg-2{display:grid;grid-template-columns:1fr 1fr;padding:4px;border-radius:12px;background:var(--bg);border:1px solid var(--line);gap:4px}
.seg-btn{height:34px;border-radius:9px;display:flex;align-items:center;justify-content:center;font:600 12px/1 var(--sans);color:var(--muted);white-space:nowrap}
.seg-btn.active{background:var(--em);color:#04150E}
.field{display:flex;flex-direction:column;gap:4px}
.field label{font:600 10.5px/1 var(--sans);color:var(--txt2);white-space:nowrap}
.input-box{height:40px;border-radius:10px;background:var(--bg);border:1px solid var(--line);padding:0 11px;display:flex;align-items:center;justify-content:space-between;font:500 12.5px/1 var(--sans);color:var(--txt);white-space:nowrap;overflow:hidden}
.input-box.mono{font-family:var(--mono);color:var(--em-b)}
.input-box.ph{color:var(--muted)}
.otp-row{display:grid;grid-template-columns:repeat(6,1fr);gap:7px}
.otp-cell{height:44px;border-radius:10px;background:var(--bg);border:1.5px solid var(--line);display:grid;place-items:center;font:600 19px/1 var(--mono);color:#fff}
.otp-cell.filled{border-color:var(--em);background:rgba(16,185,129,.08);color:var(--em-b)}
.otp-cell.cursor{border-color:var(--gold-b)}

/* Cards & Lists */
.card{border-radius:16px;background:var(--surf);border:1px solid var(--line);padding:13px}
.card.selected{border-color:var(--em-b);background:linear-gradient(150deg,rgba(16,185,129,.12),var(--surf))}
.tag{display:inline-flex;align-items:center;gap:5px;height:22px;padding:0 8px;border-radius:999px;font:600 10.5px/1 var(--sans);font-variant-numeric:tabular-nums;white-space:nowrap}
.tag-em{background:var(--em-bg);color:var(--em-b);border:1px solid var(--line-s)}
.tag-gold{background:var(--gold-bg);color:var(--gold-b);border:1px solid rgba(251,191,36,.32)}
.tag-sky{background:var(--sky-bg);color:var(--sky);border:1px solid rgba(56,189,248,.3)}
.tag-red{background:var(--red-bg);color:var(--red);border:1px solid rgba(248,113,113,.3)}
.tag-muted{background:var(--surf2);color:var(--txt2);border:1px solid var(--line)}

/* Operation & Request Cards */
.day-hdr{display:flex;align-items:center;justify-content:space-between;font:600 11px/1 var(--mono);color:var(--muted);padding:2px 2px 0;text-transform:uppercase;letter-spacing:.04em}
.op-card{border-radius:14px;background:var(--surf);border:1px solid var(--line);padding:10px 12px;display:flex;align-items:center;gap:10px;position:relative;overflow:hidden}
.op-card::before{content:"";position:absolute;left:0;top:8px;bottom:8px;width:3px;border-radius:0 3px 3px 0;background:var(--em-b)}
.op-card.out::before{background:var(--gold-b)}
.op-card.move::before{background:var(--sky)}
.op-card.burn::before{background:var(--red)}
.op-main{min-width:0;flex:1}
.op-top{display:flex;align-items:center;justify-content:space-between;gap:8px}
.op-title{font:600 12.5px/1.2 var(--sans);color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.op-delta{font:600 12px/1 var(--mono);white-space:nowrap;flex:none}
.op-meta{margin-top:3px;font:400 11px/1.2 var(--sans);color:var(--muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}

.req-card{border-radius:15px;background:var(--surf);border:1px solid var(--line);padding:12px;display:flex;flex-direction:column;gap:9px}
.req-head{display:flex;align-items:center;justify-content:space-between;gap:8px}
.steps-4{display:grid;grid-template-columns:repeat(4,1fr);gap:4px}
.st-seg{height:5px;border-radius:99px;background:var(--surf3)}
.st-seg.on{background:var(--em-b)}
.st-seg.cur{background:var(--gold-b)}
.st-labels{display:grid;grid-template-columns:repeat(4,1fr);gap:4px;font:500 9.5px/1 var(--sans);color:var(--muted);text-align:center}
.st-labels span{white-space:nowrap}
.st-labels .on{color:var(--em-b);font-weight:600}
.st-labels .cur{color:var(--gold-b);font-weight:600}

/* Tariff 3-col equal cards */
.tariffs-3{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px}
.tariff{border-radius:13px;background:var(--surf2);border:1.5px solid var(--line);padding:10px 6px;display:flex;flex-direction:column;align-items:center;gap:4px;text-align:center}
.tariff.active{border-color:var(--gold-b);background:rgba(245,158,11,.13)}
.tariff .t-days{font:600 11.5px/1 var(--sans);color:var(--txt);white-space:nowrap}
.tariff .t-price{font:600 16px/1 var(--sans);font-variant-numeric:tabular-nums;color:#fff;white-space:nowrap}
.tariff.active .t-price{color:var(--gold-b)}
.tariff .t-save{font:600 10px/1 var(--sans);font-variant-numeric:tabular-nums;color:var(--em-b);white-space:nowrap}
`;

function navBar(activeIdx, reqBadge = 2) {
  const items = [
    { label: 'Главная', icon: IC.home },
    { label: 'Журнал', icon: IC.hist },
    { label: 'Заявки', icon: IC.req, badge: reqBadge },
    { label: 'Каталог', icon: IC.cat },
    { label: 'Ещё', icon: IC.more }
  ];
  return `
  <div class="nav-wrap">
    <div class="nav-pill" data-equal-row="nav">
      ${items.map((it, idx) => `
        <div class="nav-item no-wrap ${idx === activeIdx ? 'active' : ''}">
          <span class="${it.badge ? 'nav-badge' : ''}">${it.icon}${it.badge ? `<i>${it.badge}</i>` : ''}</span>
          <span class="no-wrap">${it.label}</span>
        </div>
      `).join('')}
    </div>
  </div>`;
}

function shell(content, navIdx = 0, showNav = true) {
  return `<!doctype html><html lang="ru"><head><meta charset="utf-8"><style>${BASE_CSS}</style></head><body>
  <div class="phone">
    <div class="statusbar">
      <span>10:42</span>
      <span class="sb-icons"><span>5G</span><span>LTE</span><span>94%</span></span>
    </div>
    <div class="body">${content}</div>
    ${showNav ? navBar(navIdx) : ''}
    <div class="gesture"></div>
  </div></body></html>`;
}

const SCREENS = [
  {
    file: '01-email-auth.png',
    title: '01 · Вход и регистрация по Email (без Google)',
    html: shell(`
      <div style="display:flex;align-items:center;justify-content:space-between;padding:4px 0 2px">
        <div style="display:flex;align-items:center;gap:10px">
          <div style="width:40px;height:40px;border-radius:12px;background:linear-gradient(135deg,#34D399,#059669);color:#04150E;display:grid;place-items:center">${IC.box}</div>
          <div>
            <div style="font:600 18px/1.1 var(--sans);color:#fff;letter-spacing:-.02em">КАПТЁРКА <span style="color:var(--gold-b)">PRO</span></div>
            <div style="font:500 10.5px/1.2 var(--mono);color:var(--muted)">v4.0 · Только по Email · Без Google</div>
          </div>
        </div>
        <span class="tag tag-em no-wrap">152-ФЗ</span>
      </div>

      <div class="card" style="display:flex;flex-direction:column;gap:10px">
        <div class="seg-2" data-equal-row="auth-seg">
          <div class="seg-btn active no-wrap">Регистрация</div>
          <div class="seg-btn no-wrap">Войти по Email</div>
        </div>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px">
          <div style="height:28px;border-radius:8px;background:var(--em-bg);border:1px solid var(--line-s);display:flex;align-items:center;justify-content:center;gap:6px;font:600 10.5px/1 var(--sans);color:var(--em-b)" class="no-wrap">1 · Почта и имя</div>
          <div style="height:28px;border-radius:8px;background:var(--surf2);border:1px solid var(--line);display:flex;align-items:center;justify-content:center;gap:6px;font:600 10.5px/1 var(--sans);color:var(--muted)" class="no-wrap">2 · Код 6 цифр</div>
        </div>

        <div class="field">
          <label>Имя или позывной</label>
          <div class="input-box">Алексей Смирнов</div>
        </div>
        <div class="field">
          <label>Электронная почта (Email)</label>
          <div class="input-box"><span>alexey@workshop.ru</span><span style="color:var(--em-b)">${IC.mail}</span></div>
        </div>
        <div class="field">
          <label>Название склада / организации</label>
          <div class="input-box">Мастерская № 1</div>
        </div>
        <div class="field">
          <label>Код группы (для 2-го телефона — по желанию)</label>
          <div class="input-box mono"><span>Автоматически при создании</span><span style="color:var(--em-b)">${IC.qr}</span></div>
        </div>

        <div style="display:flex;align-items:center;gap:9px;padding:8px 10px;border-radius:10px;background:var(--bg);border:1px solid var(--line)">
          <div style="width:18px;height:18px;border-radius:5px;background:var(--em);color:#04150E;display:grid;place-items:center;flex:none">${IC.check}</div>
          <div style="font:400 11px/1.3 var(--sans);color:var(--txt2)">Принимаю <span style="color:var(--em-b);text-decoration:underline">Оферту</span> и <span style="color:var(--em-b);text-decoration:underline">Политику (152-ФЗ)</span></div>
        </div>

        <div class="btn btn-em no-wrap">${IC.mail}<span>Получить 6-значный код на Email</span></div>
      </div>

      <!-- Preview of Step 2 OTP confirmation sheet -->
      <div class="card" style="border-color:var(--line-s);display:flex;flex-direction:column;gap:9px;background:linear-gradient(155deg,#122229,var(--surf))">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span style="font:600 12.5px/1 var(--sans);color:#fff" class="no-wrap">Шаг 2 · Подтверждение кода из письма</span>
          <span class="tag tag-gold no-wrap">10 мин</span>
        </div>
        <div class="otp-row" data-equal-row="otp">
          <div class="otp-cell filled">4</div>
          <div class="otp-cell filled">8</div>
          <div class="otp-cell filled">2</div>
          <div class="otp-cell cursor">9</div>
          <div class="otp-cell"></div>
          <div class="otp-cell"></div>
        </div>
        <div style="display:flex;align-items:center;justify-content:space-between;font:500 10.5px/1 var(--sans);color:var(--muted)">
          <span class="no-wrap">Повтор через 48 сек</span>
          <span style="color:var(--em-b);font-weight:600" class="no-wrap">Изменить Email</span>
        </div>
      </div>

      <div class="btn btn-ghost no-wrap" style="height:40px;font-size:12px">${IC.wifiOff}<span>Войти автономно (офлайн без сети)</span></div>
    `, 0, false)
  },
  {
    file: '02-profile-onboarding.png',
    title: '02 · Выбор стартового профиля (Путь А)',
    html: shell(`
      <div style="padding:4px 0 2px">
        <span class="tag tag-em no-wrap">Первый запуск · Путь А</span>
        <div style="margin-top:8px;font:600 21px/1.15 var(--sans);color:#fff;letter-spacing:-.02em">Выберите стартовый профиль</div>
        <div style="margin-top:4px;font:400 12.5px/1.4 var(--sans);color:var(--muted)">Подстроим справочник, склады и бланки Excel под вашу работу. Сменить структуру можно в любой момент.</div>
      </div>

      <div style="display:flex;flex-direction:column;gap:10px;flex:1">
        <div class="card selected" style="display:flex;flex-direction:column;gap:8px">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:8px">
            <div style="display:flex;align-items:center;gap:10px;min-width:0">
              <div class="acc-ico">${IC.wrench}</div>
              <div style="min-width:0">
                <div style="font:600 14px/1.2 var(--sans);color:#fff" class="no-wrap">Мастерская / бригада / склад</div>
                <div style="font:500 10.5px/1.2 var(--mono);color:var(--em-b)" class="no-wrap">Гражданский универсальный учёт</div>
              </div>
            </div>
            <div style="width:20px;height:20px;border-radius:50%;background:var(--em);color:#04150E;display:grid;place-items:center;flex:none">${IC.check}</div>
          </div>
          <div style="font:400 11.5px/1.4 var(--sans);color:var(--txt2)">Нейтральный справочник: электроинструмент, расходники, СИЗ, запчасти и ГСМ. Обычные бланки выдачи и остатков в Excel.</div>
          <div style="display:flex;gap:6px;flex-wrap:nowrap">
            <span class="tag tag-em no-wrap">3 склада</span>
            <span class="tag tag-muted no-wrap">65 карточек</span>
            <span class="tag tag-muted no-wrap">Ведомость XLSX</span>
          </div>
        </div>

        <div class="card" style="display:flex;flex-direction:column;gap:8px">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:8px">
            <div style="display:flex;align-items:center;gap:10px;min-width:0">
              <div class="acc-ico muted" style="color:var(--gold-b);background:var(--gold-bg)">${IC.shield}</div>
              <div style="min-width:0">
                <div style="font:600 14px/1.2 var(--sans);color:#fff" class="no-wrap">Склад подразделения / снабжение</div>
                <div style="font:600 11px/1.2 var(--sans);color:var(--gold-b)" class="no-wrap">Ведомственный справочник + ф. № 8 / 18</div>
              </div>
            </div>
            <div style="width:20px;height:20px;border-radius:50%;border:1.5px solid var(--line);flex:none"></div>
          </div>
          <div style="font:400 11.5px/1.4 var(--sans);color:var(--txt2)">Полный штатный справочник по службам (РАВ, БПЛА, Связь, Вещевая, ГСМ, Мед), КЗ, передовые точки и Формы № 8 и № 18.</div>
          <div style="display:flex;gap:6px;flex-wrap:nowrap">
            <span class="tag tag-gold no-wrap">КЗ + точки</span>
            <span class="tag tag-muted no-wrap">Формы № 8 / 18</span>
            <span class="tag tag-muted no-wrap">Заявки</span>
          </div>
        </div>

        <div class="card" style="display:flex;flex-direction:column;gap:8px">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:8px">
            <div style="display:flex;align-items:center;gap:10px;min-width:0">
              <div class="acc-ico muted" style="color:var(--sky);background:var(--sky-bg)">${IC.blank}</div>
              <div style="min-width:0">
                <div style="font:600 14px/1.2 var(--sans);color:#fff" class="no-wrap">Пустой склад (с нуля)</div>
                <div style="font:500 10.5px/1.2 var(--mono);color:var(--sky)" class="no-wrap">Только ваши категории и карточки</div>
              </div>
            </div>
            <div style="width:20px;height:20px;border-radius:50%;border:1.5px solid var(--line);flex:none"></div>
          </div>
          <div style="font:400 11.5px/1.4 var(--sans);color:var(--txt2)">Один чистый «Основной склад» без предустановленных позиций — удобно, если хотите сразу внести свою номенклатуру.</div>
          <div style="display:flex;gap:6px;flex-wrap:nowrap">
            <span class="tag tag-sky no-wrap">1 склад</span>
            <span class="tag tag-muted no-wrap">0 лишних строк</span>
            <span class="tag tag-muted no-wrap">Чистый старт</span>
          </div>
        </div>
      </div>

      <div style="padding:10px 12px;border-radius:12px;background:rgba(16,185,129,.08);border:1px solid var(--line-s);display:flex;align-items:center;gap:8px;font:500 11px/1.35 var(--sans);color:#CDEBE0">
        <span style="color:var(--em-b)">${IC.shield}</span>
        <span>При обновлении уже установленной версии старые склады, остатки и журнал не меняются.</span>
      </div>

      <div class="btn btn-em no-wrap"><span>Начать работу</span></div>
    `, 0, false)
  },
  {
    file: '03-dashboard-workshop.png',
    title: '03 · Главная (Профиль: Мастерская / бригада)',
    html: shell(`
      <div class="top-head">
        <div class="top-title-wrap">
          <div class="top-title">Мастерская № 1</div>
          <div class="top-sub">Каптёрка PRO · kapt_8f3a…c9</div>
        </div>
        <div class="prof-pill">
          <div>
            <div class="p-name no-wrap">Алексей</div>
            <div class="p-sub no-wrap">PRO · 28 дн.</div>
          </div>
          <div class="avatar">А</div>
        </div>
      </div>

      <div class="grid-4" data-equal-row="svc-w">
        <div class="svc-tile"><span class="dot"></span>${IC.sync}<span class="lbl no-wrap">Синхр.</span><span class="sub no-wrap">в сети</span></div>
        <div class="svc-tile">${IC.qr}<span class="lbl no-wrap">QR-код</span><span class="sub no-wrap" style="color:var(--muted)">2 тел.</span></div>
        <div class="svc-tile">${IC.excel}<span class="lbl no-wrap">Отчёты</span><span class="sub no-wrap" style="color:var(--muted)">XLSX</span></div>
        <div class="svc-tile">${IC.help}<span class="lbl no-wrap">Помощь</span><span class="sub no-wrap" style="color:var(--muted)">гид</span></div>
      </div>

      <div class="grid-4" data-equal-row="ops-w">
        <div class="op-btn op-in">${IC.in}<span class="no-wrap">Приход</span></div>
        <div class="op-btn op-out">${IC.out}<span class="no-wrap">Выдать</span></div>
        <div class="op-btn op-move">${IC.move}<span class="no-wrap">Перенос</span></div>
        <div class="op-btn op-burn">${IC.burn}<span class="no-wrap">Списать</span></div>
      </div>

      <div class="search-bar">${IC.search}<span>Поиск по складам и инструменту…</span></div>

      <div class="chips">
        <div class="chip active no-wrap">Все (27)</div>
        <div class="chip no-wrap">Инструмент</div>
        <div class="chip no-wrap">Расходники</div>
        <div class="chip no-wrap">СИЗ</div>
        <div class="chip no-wrap">ГСМ</div>
      </div>

      <div class="acc-list">
        <div class="acc open">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico">${IC.box}</div>
              <div style="min-width:0">
                <div class="acc-name">Основной склад</div>
                <div class="acc-meta">14 позиций · базовый</div>
              </div>
            </div>
            <div class="acc-right">
              <span class="pill-num no-wrap">186 шт.</span>
              <span class="acc-chev">${IC.chevDown}</span>
            </div>
          </div>
          <div class="tbl-head"><span>Наименование</span><span>Остаток</span><span>Изм.</span></div>
          <div class="tbl-row">
            <div style="min-width:0"><div class="tbl-name">Перфоратор Bosch SDS-Plus</div><div class="tbl-cat">Электроинструмент</div></div>
            <div class="tbl-qty">6<small>шт.</small></div>
            <div class="tbl-adj no-wrap">±</div>
          </div>
          <div class="tbl-row">
            <div style="min-width:0"><div class="tbl-name">Диски отрезные 125 мм</div><div class="tbl-cat">Расходники</div></div>
            <div class="tbl-qty">35<small>уп.</small></div>
            <div class="tbl-adj no-wrap">±</div>
          </div>
          <div class="tbl-row">
            <div style="min-width:0"><div class="tbl-name">Краги сварочные усил.</div><div class="tbl-cat">СИЗ и спецодежда</div></div>
            <div class="tbl-qty">24<small>пар</small></div>
            <div class="tbl-adj no-wrap">±</div>
          </div>
          <div class="tbl-row">
            <div style="min-width:0"><div class="tbl-name">Шуруповёрт Makita 18В</div><div class="tbl-cat">Электроинструмент</div></div>
            <div class="tbl-qty">4<small>шт.</small></div>
            <div class="tbl-adj no-wrap">±</div>
          </div>
          <div class="tbl-row">
            <div style="min-width:0"><div class="tbl-name">Анкер-клин 6×40 (короб)</div><div class="tbl-cat">Крепёж и метизы</div></div>
            <div class="tbl-qty">18<small>кор.</small></div>
            <div class="tbl-adj no-wrap">±</div>
          </div>
        </div>

        <div class="acc">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico muted">${IC.wrench}</div>
              <div style="min-width:0">
                <div class="acc-name">Цех / Бригада № 2</div>
                <div class="acc-meta">8 позиций · в работе</div>
              </div>
            </div>
            <div class="acc-right">
              <span class="pill-num no-wrap">42 шт.</span>
              <span class="acc-chev">${IC.chevRight}</span>
            </div>
          </div>
        </div>

        <div class="acc">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico muted">${IC.move}</div>
              <div style="min-width:0">
                <div class="acc-name">Выездной комплект</div>
                <div class="acc-meta">5 позиций · фургон</div>
              </div>
            </div>
            <div class="acc-right">
              <span class="pill-num no-wrap">19 шт.</span>
              <span class="acc-chev">${IC.chevRight}</span>
            </div>
          </div>
        </div>
      </div>
    `, 0, true)
  },
  {
    file: '04-dashboard-unit.png',
    title: '04 · Главная (Профиль: Склад подразделения)',
    html: shell(`
      <div class="top-head">
        <div class="top-title-wrap">
          <div class="top-title">2-я Рота снабжения</div>
          <div class="top-sub">Каптёрка PRO · kapt_4c1b…9a</div>
        </div>
        <div class="prof-pill">
          <div>
            <div class="p-name no-wrap">Сокол</div>
            <div class="p-sub gold no-wrap">Демо · 3 дн.</div>
          </div>
          <div class="avatar" style="background:var(--gold-bg);border-color:rgba(251,191,36,.35);color:var(--gold-b)">С</div>
        </div>
      </div>

      <div class="mini-banner">
        <span>Демо: осталось 3 дн. · далее 1 склад бесплатно</span>
        <b class="no-wrap">PRO 490 ₽</b>
      </div>

      <div class="grid-4" data-equal-row="svc-u">
        <div class="svc-tile"><span class="dot"></span>${IC.sync}<span class="lbl no-wrap">Синхр.</span><span class="sub no-wrap">1 мин</span></div>
        <div class="svc-tile">${IC.qr}<span class="lbl no-wrap">QR-код</span><span class="sub no-wrap" style="color:var(--muted)">3 тел.</span></div>
        <div class="svc-tile">${IC.excel}<span class="lbl no-wrap">Ф. 8 / 18</span><span class="sub no-wrap" style="color:var(--muted)">Excel</span></div>
        <div class="svc-tile">${IC.help}<span class="lbl no-wrap">Помощь</span><span class="sub no-wrap" style="color:var(--muted)">гид</span></div>
      </div>

      <div class="grid-4" data-equal-row="ops-u">
        <div class="op-btn op-in">${IC.in}<span class="no-wrap">Приход</span></div>
        <div class="op-btn op-out">${IC.out}<span class="no-wrap">Выдать</span></div>
        <div class="op-btn op-move">${IC.move}<span class="no-wrap">Перенос</span></div>
        <div class="op-btn op-burn">${IC.burn}<span class="no-wrap">Списать</span></div>
      </div>

      <div class="chips">
        <div class="chip active no-wrap">Все службы</div>
        <div class="chip no-wrap">РАВ</div>
        <div class="chip no-wrap">БПЛА</div>
        <div class="chip no-wrap">Связь</div>
        <div class="chip no-wrap">Вещевая</div>
      </div>

      <div class="acc-list">
        <div class="acc open">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico">${IC.shield}</div>
              <div style="min-width:0">
                <div class="acc-name">Базовый склад (КЗ)</div>
                <div class="acc-meta">18 позиций · основной</div>
              </div>
            </div>
            <div class="acc-right">
              <span class="pill-num no-wrap">412 шт.</span>
              <span class="acc-chev">${IC.chevDown}</span>
            </div>
          </div>
          <div class="tbl-head"><span>Наименование</span><span>Остаток</span><span>Изм.</span></div>
          <div class="tbl-row">
            <div style="min-width:0"><div class="tbl-name">Радиостанция Азарт</div><div class="tbl-cat">Служба связи и РЭБ</div></div>
            <div class="tbl-qty">14<small>шт.</small></div>
            <div class="tbl-adj no-wrap">±</div>
          </div>
          <div class="tbl-row">
            <div style="min-width:0"><div class="tbl-name">Mavic 3T (Тепловизор)</div><div class="tbl-cat">Служба БПЛА</div></div>
            <div class="tbl-qty">5<small>шт.</small></div>
            <div class="tbl-adj no-wrap">±</div>
          </div>
          <div class="tbl-row">
            <div style="min-width:0"><div class="tbl-name">Аптечка 1-го эшелона</div><div class="tbl-cat">Медицинская служба</div></div>
            <div class="tbl-qty">48<small>шт.</small></div>
            <div class="tbl-adj no-wrap">±</div>
          </div>
          <div class="tbl-row">
            <div style="min-width:0"><div class="tbl-name">Бронешлем 6Б47</div><div class="tbl-cat">Вещевая и СИБЗ</div></div>
            <div class="tbl-qty">22<small>шт.</small></div>
            <div class="tbl-adj no-wrap">±</div>
          </div>
        </div>

        <div class="acc">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico muted">${IC.move}</div>
              <div style="min-width:0">
                <div class="acc-name">Передовая точка (ЛБС)</div>
                <div class="acc-meta">9 позиций · выдача</div>
              </div>
            </div>
            <div class="acc-right">
              <span class="pill-num no-wrap">95 шт.</span>
              <span class="acc-chev">${IC.chevRight}</span>
            </div>
          </div>
        </div>

        <div class="acc">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico muted">${IC.box}</div>
              <div style="min-width:0">
                <div class="acc-name">Медпункт</div>
                <div class="acc-meta">6 позиций · резерв</div>
              </div>
            </div>
            <div class="acc-right">
              <span class="pill-num no-wrap">64 шт.</span>
              <span class="acc-chev">${IC.chevRight}</span>
            </div>
          </div>
        </div>
      </div>
    `, 0, true)
  },
  {
    file: '05-catalog-accordion.png',
    title: '05 · Каталог / Имущество (раскрывающиеся группы)',
    html: shell(`
      <div class="top-head">
        <div class="top-title-wrap">
          <div class="top-title">Каталог имущества</div>
          <div class="top-sub">65 карточек · 6 групп · свернуть всё</div>
        </div>
        <div class="btn btn-em btn-compact no-wrap" style="width:auto">${IC.plus}<span>Карточка</span></div>
      </div>

      <div class="search-bar">${IC.search}<span>Найти по названию или категории…</span></div>

      <div class="chips">
        <div class="chip active no-wrap">Все (65)</div>
        <div class="chip no-wrap">С остатком (27)</div>
        <div class="chip no-wrap">Свои (8)</div>
        <div class="chip no-wrap">+ Группа</div>
      </div>

      <div class="acc-list">
        <div class="acc open">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico">${IC.wrench}</div>
              <div style="min-width:0">
                <div class="acc-name">Электроинструмент</div>
                <div class="acc-meta">Перфораторы, УШМ, шуруповёрты</div>
              </div>
            </div>
            <div class="acc-right">
              <span class="pill-num no-wrap">12 поз.</span>
              <span class="acc-chev">${IC.chevDown}</span>
            </div>
          </div>
          <div class="tbl-row" style="grid-template-columns:1fr 66px 24px">
            <div style="min-width:0"><div class="tbl-name">Перфоратор Bosch SDS-Plus</div><div class="tbl-cat">Сетевой · ед. шт.</div></div>
            <div class="tbl-qty" style="color:var(--em-b)">6<small>шт.</small></div>
            <span class="acc-chev">${IC.chevRight}</span>
          </div>
          <div class="tbl-row" style="grid-template-columns:1fr 66px 24px">
            <div style="min-width:0"><div class="tbl-name">УШМ 125 мм (болгарка)</div><div class="tbl-cat">Сетевой · ед. шт.</div></div>
            <div class="tbl-qty" style="color:var(--em-b)">5<small>шт.</small></div>
            <span class="acc-chev">${IC.chevRight}</span>
          </div>
          <div class="tbl-row" style="grid-template-columns:1fr 66px 24px">
            <div style="min-width:0"><div class="tbl-name">Шуруповёрт Makita 18В</div><div class="tbl-cat">Аккумуляторный · ед. шт.</div></div>
            <div class="tbl-qty" style="color:var(--em-b)">4<small>шт.</small></div>
            <span class="acc-chev">${IC.chevRight}</span>
          </div>
          <div class="tbl-row" style="grid-template-columns:1fr 66px 24px">
            <div style="min-width:0"><div class="tbl-name">Лазерный уровень 360°</div><div class="tbl-cat">Измерение · ед. шт.</div></div>
            <div class="tbl-qty" style="color:var(--muted)">0<small>шт.</small></div>
            <span class="acc-chev">${IC.chevRight}</span>
          </div>
        </div>

        <div class="acc open">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico" style="background:var(--gold-bg);color:var(--gold-b)">${IC.box}</div>
              <div style="min-width:0">
                <div class="acc-name">Расходные материалы</div>
                <div class="acc-meta">Диски, буры, электроды, пена</div>
              </div>
            </div>
            <div class="acc-right">
              <span class="pill-num no-wrap">16 поз.</span>
              <span class="acc-chev">${IC.chevDown}</span>
            </div>
          </div>
          <div class="tbl-row" style="grid-template-columns:1fr 66px 24px">
            <div style="min-width:0"><div class="tbl-name">Диски отрезные 125 мм</div><div class="tbl-cat">Металл · ед. уп.</div></div>
            <div class="tbl-qty" style="color:var(--em-b)">35<small>уп.</small></div>
            <span class="acc-chev">${IC.chevRight}</span>
          </div>
          <div class="tbl-row" style="grid-template-columns:1fr 66px 24px">
            <div style="min-width:0"><div class="tbl-name">Электроды ОК-46 (3 мм)</div><div class="tbl-cat">Сварка · ед. пач.</div></div>
            <div class="tbl-qty" style="color:var(--em-b)">12<small>пач.</small></div>
            <span class="acc-chev">${IC.chevRight}</span>
          </div>
        </div>

        <div class="acc">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico muted">${IC.shield}</div>
              <div style="min-width:0"><div class="acc-name">Спецодежда и СИЗ</div><div class="acc-meta">Краги, очки, каски, жилеты</div></div>
            </div>
            <div class="acc-right"><span class="pill-num no-wrap">14 поз.</span><span class="acc-chev">${IC.chevRight}</span></div>
          </div>
        </div>

        <div class="acc">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico muted">${IC.move}</div>
              <div style="min-width:0"><div class="acc-name">ГСМ, масла и химия</div><div class="acc-meta">WD-40, масла, герметики</div></div>
            </div>
            <div class="acc-right"><span class="pill-num no-wrap">11 поз.</span><span class="acc-chev">${IC.chevRight}</span></div>
          </div>
        </div>
      </div>
    `, 3, true)
  },
  {
    file: '06-operations.png',
    title: '06 · Журнал операций (ровные фильтры и группы по дням)',
    html: shell(`
      <div class="top-head">
        <div class="top-title-wrap">
          <div class="top-title">Журнал операций</div>
          <div class="top-sub">34 записи · синхронизировано</div>
        </div>
        <div class="btn btn-ghost btn-compact no-wrap" style="width:auto">${IC.excel}<span>Excel</span></div>
      </div>

      <div class="search-bar">${IC.search}<span>Поиск по позиции, фамилии, складу…</span></div>

      <div class="grid-4" data-equal-row="period">
        <div class="seg-btn active no-wrap" style="height:30px;font-size:11px">Сегодня</div>
        <div class="seg-btn no-wrap" style="height:30px;font-size:11px;background:var(--surf);border:1px solid var(--line)">7 дней</div>
        <div class="seg-btn no-wrap" style="height:30px;font-size:11px;background:var(--surf);border:1px solid var(--line)">30 дней</div>
        <div class="seg-btn no-wrap" style="height:30px;font-size:11px;background:var(--surf);border:1px solid var(--line)">Все</div>
      </div>

      <div class="chips">
        <div class="chip active no-wrap">Все типы</div>
        <div class="chip no-wrap">Приход</div>
        <div class="chip no-wrap">Выдача</div>
        <div class="chip no-wrap">Перенос</div>
        <div class="chip no-wrap">Списание</div>
      </div>

      <div style="display:flex;flex-direction:column;gap:8px;flex:1">
        <div class="day-hdr"><span>Сегодня · 22 ноября</span><span>4 операции</span></div>

        <div class="op-card out">
          <div class="op-main">
            <div class="op-top"><span class="op-title">Выдача на руки · Петров А.С.</span><span class="op-delta" style="color:var(--gold-b)">−2 шт.</span></div>
            <div class="op-meta">Перфоратор Bosch SDS-Plus · Основной склад · 10:18</div>
          </div>
        </div>

        <div class="op-card">
          <div class="op-main">
            <div class="op-top"><span class="op-title">Приход · ВсеИнструменты</span><span class="op-delta" style="color:var(--em-b)">+20 уп.</span></div>
            <div class="op-meta">Диски отрезные 125 мм · Основной склад · 09:40</div>
          </div>
        </div>

        <div class="op-card move">
          <div class="op-main">
            <div class="op-top"><span class="op-title">Перенос · Цех → Фургон</span><span class="op-delta" style="color:var(--sky)">3 поз.</span></div>
            <div class="op-meta">Шуруповёрт 18В, краги, рулетка · 08:55</div>
          </div>
        </div>

        <div class="op-card burn">
          <div class="op-main">
            <div class="op-top"><span class="op-title">Списание · Износ редуктора</span><span class="op-delta" style="color:var(--red)">−1 шт.</span></div>
            <div class="op-meta">УШМ 125 мм (старая) · Цех / Бригада № 2 · 08:20</div>
          </div>
        </div>

        <div class="day-hdr" style="margin-top:4px"><span>Вчера · 21 ноября</span><span>6 операций</span></div>

        <div class="op-card out">
          <div class="op-main">
            <div class="op-top"><span class="op-title">Выдача на руки · Иванов Д.К.</span><span class="op-delta" style="color:var(--gold-b)">−4 пар</span></div>
            <div class="op-meta">Краги сварочные усил. · Основной склад · 17:35</div>
          </div>
        </div>

        <div class="op-card">
          <div class="op-main">
            <div class="op-top"><span class="op-title">Корректировка · Сверка остатков</span><span class="op-delta" style="color:var(--em-b)">+2 кор.</span></div>
            <div class="op-meta">Анкер-клин 6×40 · Основной склад · 16:12</div>
          </div>
        </div>
      </div>
    `, 1, true)
  },
  {
    file: '07-requests.png',
    title: '07 · Заявки по этапам (компактные карточки и ровные кнопки)',
    html: shell(`
      <div class="top-head">
        <div class="top-title-wrap">
          <div class="top-title">Заявки на выдачу</div>
          <div class="top-sub">2 активных · 14 выполнено</div>
        </div>
        <div class="btn btn-em btn-compact no-wrap" style="width:auto">${IC.plus}<span>Заявка</span></div>
      </div>

      <div class="grid-4" data-equal-row="req-tabs">
        <div class="seg-btn active no-wrap" style="height:32px;font-size:11px">Все (4)</div>
        <div class="seg-btn no-wrap" style="height:32px;font-size:11px;background:var(--surf);border:1px solid var(--line)">Новые (1)</div>
        <div class="seg-btn no-wrap" style="height:32px;font-size:11px;background:var(--surf);border:1px solid var(--line)">Сборка (1)</div>
        <div class="seg-btn no-wrap" style="height:32px;font-size:11px;background:var(--surf);border:1px solid var(--line)">Выданы</div>
      </div>

      <div style="display:flex;flex-direction:column;gap:10px;flex:1">
        <div class="req-card" style="border-color:rgba(251,191,36,.36)">
          <div class="req-head">
            <div style="min-width:0">
              <div style="font:600 13.5px/1.2 var(--sans);color:#fff" class="no-wrap">Бригада № 2 · Объект «Север»</div>
              <div style="font:400 11px/1.2 var(--sans);color:var(--muted)">Заявитель: Петров А.С. · сегодня 10:05</div>
            </div>
            <span class="tag tag-gold no-wrap">Сборка</span>
          </div>
          <div class="steps-4"><div class="st-seg on"></div><div class="st-seg cur"></div><div class="st-seg"></div><div class="st-seg"></div></div>
          <div class="st-labels"><span class="on">Новая</span><span class="cur">Сборка</span><span>Собрана</span><span>Выдана</span></div>
          <div style="padding:8px 10px;border-radius:10px;background:var(--bg);border:1px solid var(--line);font:500 11.5px/1.45 var(--sans);color:var(--txt2)">
            • Диски отрезные 125 мм — <b style="color:#fff">5 уп.</b><br>
            • Краги сварочные — <b style="color:#fff">3 пар</b> · Анкер 6×40 — <b style="color:#fff">2 кор.</b>
          </div>
          <div class="btn-row-2" data-equal-row="req-act-1">
            <div class="btn btn-ghost btn-sm no-wrap">Собрана</div>
            <div class="btn btn-em btn-sm no-wrap">Выдать со склада</div>
          </div>
        </div>

        <div class="req-card">
          <div class="req-head">
            <div style="min-width:0">
              <div style="font:600 13.5px/1.2 var(--sans);color:#fff" class="no-wrap">Выездной комплект · Фургон</div>
              <div style="font:400 11px/1.2 var(--sans);color:var(--muted)">Заявитель: Сидоров М. · сегодня 08:30</div>
            </div>
            <span class="tag tag-sky no-wrap">Новая</span>
          </div>
          <div class="steps-4"><div class="st-seg cur"></div><div class="st-seg"></div><div class="st-seg"></div><div class="st-seg"></div></div>
          <div class="st-labels"><span class="cur">Новая</span><span>Сборка</span><span>Собрана</span><span>Выдана</span></div>
          <div style="padding:8px 10px;border-radius:10px;background:var(--bg);border:1px solid var(--line);font:500 11.5px/1.45 var(--sans);color:var(--txt2)">
            • Шуруповёрт Makita 18В — <b style="color:#fff">1 шт.</b><br>
            • Электроды ОК-46 — <b style="color:#fff">2 пач.</b>
          </div>
          <div class="btn-row-2" data-equal-row="req-act-2">
            <div class="btn btn-ghost btn-sm no-wrap">Взять в сборку</div>
            <div class="btn btn-em btn-sm no-wrap">Выдать со склада</div>
          </div>
        </div>

        <div class="acc">
          <div class="acc-head">
            <div class="acc-left">
              <div class="acc-ico" style="background:var(--em-bg);color:var(--em-b)">${IC.check}</div>
              <div style="min-width:0"><div class="acc-name">Выданные заявки (14)</div><div class="acc-meta">Нажмите, чтобы раскрыть архив</div></div>
            </div>
            <span class="acc-chev">${IC.chevRight}</span>
          </div>
        </div>
      </div>
    `, 2, true)
  },
  {
    file: '08-pro-reports-qr.png',
    title: '08 · PRO (ЮKassa · НПД), Отчёты Excel и QR-подключение',
    html: shell(`
      <div class="top-head">
        <div class="top-title-wrap">
          <div class="top-title">PRO · Оплата и доступ</div>
          <div class="top-sub">ЮKassa · Самозанятый (НПД)</div>
        </div>
        <span class="tag tag-gold no-wrap">Демо 3 дн.</span>
      </div>

      <div class="card" style="border-color:rgba(251,191,36,.42);background:linear-gradient(155deg,#182022,var(--surf));display:flex;flex-direction:column;gap:10px">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <div style="display:flex;align-items:center;gap:8px;font:600 14px/1 var(--sans);color:#fff">${IC.crown}<span class="no-wrap">Лицензия Каптёрка PRO</span></div>
          <span class="tag tag-em no-wrap">Без автосписаний</span>
        </div>
        <div style="font:400 11.5px/1.4 var(--sans);color:var(--txt2)">Безлимит складов, карточек, заявок, отчётов Excel и синхронизации. Базовый режим (1 склад, 15 поз.) всегда бесплатен.</div>

        <div class="tariffs-3" data-equal-row="tariffs">
          <div class="tariff active">
            <span class="t-days no-wrap">30 дней</span>
            <span class="t-price no-wrap">490 ₽</span>
            <span class="t-save no-wrap" style="color:var(--muted)">16 ₽/день</span>
          </div>
          <div class="tariff">
            <span class="t-days no-wrap">90 дней</span>
            <span class="t-price no-wrap">1 190 ₽</span>
            <span class="t-save no-wrap">выгода −19%</span>
          </div>
          <div class="tariff">
            <span class="t-days no-wrap">365 дней</span>
            <span class="t-price no-wrap">3 490 ₽</span>
            <span class="t-save no-wrap">выгода −41%</span>
          </div>
        </div>

        <div class="btn btn-gold no-wrap"><span>Оплатить 490 ₽ · ЮKassa (СБП / МИР)</span></div>
        <div style="font:500 10px/1.35 var(--mono);color:var(--muted);text-align:center">Самозанятый (НПД, ИНН 250104230398) · Ключ KAPT-… на Email · Чек «Мой налог»</div>
      </div>

      <div class="card" style="display:flex;flex-direction:column;gap:8px">
        <div style="font:600 12.5px/1 var(--sans);color:#fff">Уже есть ключ лицензии?</div>
        <div style="display:grid;grid-template-columns:1fr auto;gap:8px">
          <div class="input-box mono" style="height:38px">KAPT-XXXX-XXXX-XXXX</div>
          <div class="btn btn-em no-wrap" style="height:38px;font-size:12px;padding:0 14px">Активировать</div>
        </div>
      </div>

      <div class="card" style="display:flex;flex-direction:column;gap:8px">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span style="font:600 12.5px/1 var(--sans);color:#fff" class="no-wrap">Отчёты Excel (.xlsx)</span>
          <span class="tag tag-muted no-wrap">Профиль: Мастерская</span>
        </div>
        <div class="btn-row-2" data-equal-row="reports-row">
          <div class="btn btn-ghost btn-sm no-wrap">${IC.excel}<span>Остатки склада</span></div>
          <div class="btn btn-ghost btn-sm no-wrap">${IC.excel}<span>Ведомость выдачи</span></div>
        </div>
      </div>

      <div class="acc">
        <div class="acc-head">
          <div class="acc-left">
            <div class="acc-ico">${IC.qr}</div>
            <div style="min-width:0"><div class="acc-name">Подключить второй телефон</div><div class="acc-meta">QR-код группы · пауза · очистка облака</div></div>
          </div>
          <span class="acc-chev">${IC.chevRight}</span>
        </div>
      </div>
    `, 4, true)
  }
];

(async () => {
  fs.mkdirSync(outDir, { recursive: true });
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.CHROME_BIN || 'C:/Program Files/Google/Chrome/Application/chrome.exe'
  });

  try {
    const ctx = await browser.newContext({ viewport: { width: 412, height: 915 }, deviceScaleFactor: 2.5 });
    const page = await ctx.newPage();

    for (const screen of SCREENS) {
      await page.setContent(screen.html, { waitUntil: 'load' });
      await page.evaluate(() => document.fonts.ready);

      // Programmatic layout verification: zero overflow, zero wrapping on .no-wrap, equal button row heights
      const report = await page.evaluate(() => {
        const issues = [];
        const phone = document.querySelector('.phone');
        if (phone.scrollWidth > phone.clientWidth + 1) {
          issues.push(`horizontal overflow: ${phone.scrollWidth} > ${phone.clientWidth}`);
        }
        if (phone.scrollHeight > phone.clientHeight + 1) {
          issues.push(`vertical clip: ${phone.scrollHeight} > ${phone.clientHeight}`);
        }
        for (const el of document.querySelectorAll('.no-wrap')) {
          const rect = el.getBoundingClientRect();
          if (el.scrollWidth > el.clientWidth + 1) {
            issues.push(`text clipped in "${el.textContent.trim()}": scrollW=${el.scrollWidth} clientW=${el.clientWidth}`);
          }
          if (rect.height > 64) {
            issues.push(`unexpectedly tall element "${el.textContent.trim()}": h=${Math.round(rect.height)}`);
          }
        }
        for (const row of document.querySelectorAll('[data-equal-row]')) {
          const heights = [...row.children].map((c) => Math.round(c.getBoundingClientRect().height));
          if (Math.max(...heights) - Math.min(...heights) > 1) {
            issues.push(`unequal row heights in ${row.getAttribute('data-equal-row')}: ${heights.join(',')}`);
          }
        }
        return issues;
      });

      if (report.length) {
        throw new Error(`Layout QA failed on ${screen.file}:\n  - ${report.join('\n  - ')}`);
      }

      await page.screenshot({ path: path.join(outDir, screen.file), fullPage: false });
      console.log(`PASS: rendered & verified ${screen.file}`);
    }

    // Build high-res 4x2 overview board (00-overview-board.png)
    const boardCtx = await browser.newContext({ viewport: { width: 1760, height: 1960 }, deviceScaleFactor: 1.5 });
    const boardPage = await boardCtx.newPage();
    const cardsHtml = SCREENS.map((s) => {
      const b64 = fs.readFileSync(path.join(outDir, s.file)).toString('base64');
      return `<figure><img src="data:image/png;base64,${b64}" alt="${s.title}"><figcaption>${s.title}</figcaption></figure>`;
    }).join('');
    await boardPage.setContent(`<!doctype html><html lang="ru"><head><meta charset="utf-8"><style>
      ${FONT_CSS}
      *{box-sizing:border-box;margin:0;padding:0}
      body{background:#060B0E;color:#F1F6F8;font-family:"IBM Plex Sans",sans-serif;padding:28px 32px}
      header{display:flex;align-items:baseline;justify-content:space-between;margin-bottom:22px;padding-bottom:14px;border-bottom:1px solid rgba(148,179,191,.2)}
      h1{font:600 26px/1.15 "IBM Plex Sans",sans-serif;color:#fff}
      header span{font:500 14px/1 "IBM Plex Mono",monospace;color:#34D399}
      .grid{display:grid;grid-template-columns:repeat(4,1fr);gap:24px}
      figure{display:flex;flex-direction:column;gap:10px;align-items:center}
      img{width:100%;height:auto;border-radius:26px;border:3px solid #1E2C37;box-shadow:0 18px 44px rgba(0,0,0,.55)}
      figcaption{font:600 13px/1.3 "IBM Plex Sans",sans-serif;color:#CFE0E8;text-align:center}
    </style></head><body>
      <header><h1>Каптёрка PRO v4.0 — Макеты нового интерфейса (Путь А · На одобрение)</h1><span>8 экранов · Ровная сетка · Без переносов · Email без Google · Самозанятый НПД</span></header>
      <div class="grid">${cardsHtml}</div>
    </body></html>`);
    await boardPage.evaluate(() => document.fonts.ready);
    await boardPage.screenshot({ path: path.join(outDir, '00-overview-board.png'), fullPage: true });
    console.log('PASS: rendered 00-overview-board.png');

    await boardCtx.close();
    await ctx.close();
  } finally {
    await browser.close();
  }
})().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
