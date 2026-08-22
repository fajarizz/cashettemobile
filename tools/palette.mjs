// OKLCH <-> sRGB <-> CIELab helpers, then M3 tonal-palette generation.

const clamp01 = (x) => Math.min(1, Math.max(0, x));

function oklchToLinearSrgb(L, C, hDeg) {
  const h = (hDeg * Math.PI) / 180;
  const a = C * Math.cos(h);
  const b = C * Math.sin(h);
  const l_ = L + 0.3963377774 * a + 0.2158037573 * b;
  const m_ = L - 0.1055613458 * a - 0.0638541728 * b;
  const s_ = L - 0.0894841775 * a - 1.291485548 * b;
  const l = l_ ** 3, m = m_ ** 3, s = s_ ** 3;
  return [
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
  ];
}

const toGamma = (c) => (c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055);
const toLinear = (c) => (c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));

function linearSrgbToXyz([r, g, b]) {
  return [
    0.4123907993 * r + 0.3575843394 * g + 0.1804807884 * b,
    0.2126390059 * r + 0.7151686788 * g + 0.0721923154 * b,
    0.0193308187 * r + 0.1191947798 * g + 0.9505321522 * b,
  ];
}

function xyzToLinearSrgb([x, y, z]) {
  return [
    3.2409699419 * x - 1.5373831776 * y - 0.4986107603 * z,
    -0.9692436363 * x + 1.8759675015 * y + 0.0415550574 * z,
    0.0556300797 * x - 0.203976959 * y + 1.0569715142 * z,
  ];
}

const WHITE = [0.9504559271, 1, 1.0890577508];

function xyzToLab([x, y, z]) {
  const f = (t) => (t > 216 / 24389 ? Math.cbrt(t) : (24389 / 27 * t + 16) / 116);
  const fx = f(x / WHITE[0]), fy = f(y / WHITE[1]), fz = f(z / WHITE[2]);
  return [116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)];
}

function labToXyz([L, a, b]) {
  const fy = (L + 16) / 116, fx = fy + a / 500, fz = fy - b / 200;
  const inv = (t) => (t ** 3 > 216 / 24389 ? t ** 3 : (116 * t - 16) * 27 / 24389);
  return [inv(fx) * WHITE[0], inv(fy) * WHITE[1], inv(fz) * WHITE[2]];
}

const hex = ([r, g, b]) =>
  "#" + [r, g, b].map((c) => Math.round(clamp01(toGamma(c)) * 255).toString(16).padStart(2, "0")).join("");

function inGamut([r, g, b]) {
  const e = -0.0001, m = 1.0001;
  return r >= e && r <= m && g >= e && g <= m && b >= e && b <= m;
}

// LCh(ab) -> hex, reducing chroma until the colour fits sRGB (M3's own approach).
function lchToHex(L, C, hDeg) {
  const h = (hDeg * Math.PI) / 180;
  let lo = 0, hi = C;
  if (inGamut(xyzToLinearSrgb(labToXyz([L, C * Math.cos(h), C * Math.sin(h)])))) lo = C;
  else {
    for (let i = 0; i < 40; i++) {
      const mid = (lo + hi) / 2;
      if (inGamut(xyzToLinearSrgb(labToXyz([L, mid * Math.cos(h), mid * Math.sin(h)])))) lo = mid;
      else hi = mid;
    }
  }
  const rgb = xyzToLinearSrgb(labToXyz([L, lo * Math.cos(h), lo * Math.sin(h)]));
  return hex(rgb.map(clamp01));
}

function oklchToLch(L, C, h) {
  const lab = xyzToLab(linearSrgbToXyz(oklchToLinearSrgb(L, C, h).map(clamp01)));
  const [Ls, a, b] = lab;
  return { L: Ls, C: Math.hypot(a, b), h: ((Math.atan2(b, a) * 180) / Math.PI + 360) % 360, hex: hex(oklchToLinearSrgb(L, C, h).map(clamp01)) };
}

function hexToLch(s) {
  const n = s.replace("#", "");
  const rgb = [0, 2, 4].map((i) => toLinear(parseInt(n.slice(i, i + 2), 16) / 255));
  const [L, a, b] = xyzToLab(linearSrgbToXyz(rgb));
  return { L, C: Math.hypot(a, b), h: ((Math.atan2(b, a) * 180) / Math.PI + 360) % 360 };
}

// ── Diagnose the web's tokens ────────────────────────────────────────────────
const WEB = {
  "background      oklch(.18 .06 116)": [0.18, 0.06, 116],
  "sidebar/card    oklch(.22 .06 116)": [0.22, 0.06, 116],
  "foreground      oklch(.95 .03 90)": [0.95, 0.03, 90],
  "ramp-2          oklch(.82 .06 95)": [0.82, 0.06, 95],
  "ramp-3          oklch(.68 .08 105)": [0.68, 0.08, 105],
  "ramp-4          oklch(.52 .08 112)": [0.52, 0.08, 112],
  "destructive     oklch(.704 .191 22)": [0.704, 0.191, 22.216],
};

console.log("=== web tokens as CIE LCh ===");
for (const [name, v] of Object.entries(WEB)) {
  const r = oklchToLch(...v);
  console.log(`${name.padEnd(36)} ${r.hex}  L*=${r.L.toFixed(1).padStart(5)}  C*=${r.C.toFixed(1).padStart(5)}  h=${r.h.toFixed(1)}`);
}
console.log("\n=== income #4ade80 ===");
const inc = hexToLch("#4ade80");
console.log(`L*=${inc.L.toFixed(1)} C*=${inc.C.toFixed(1)} h=${inc.h.toFixed(1)}`);

// ── Tonal palettes ───────────────────────────────────────────────────────────
const TONES = [0, 4, 6, 10, 12, 17, 20, 22, 24, 30, 35, 40, 50, 60, 70, 80, 87, 90, 92, 94, 95, 96, 98, 99, 100];

const PALETTES = {
  primary:       { h: 94,  C: 20 },  // cream / wheat — the web's foreground
  secondary:     { h: 118, C: 26 },  // sage olive
  tertiary:      { h: 72,  C: 58 },  // ochre — Expressive's accent energy
  neutral:       { h: 112, C: 17 },  // olive ground; gamut-clips to the web's exact dark tones
  neutralVariant:{ h: 110, C: 24 },
  error:         { h: 26,  C: 78 },
};

// Hold chroma through the dark tones (the olive ground IS the identity), release it
// toward white where sRGB cannot carry it anyway.
const taperFor = (t) => (t <= 0 || t >= 100 ? 0 : Math.min(t / 5, (100 - t) / 22, 1));

const out = {};
for (const [name, { h, C }] of Object.entries(PALETTES)) {
  out[name] = {};
  for (const t of TONES) out[name][t] = lchToHex(t, C * taperFor(t), h);
}

console.log("\n=== tonal palettes ===");
for (const [name, ramp] of Object.entries(out)) {
  console.log(`\n${name}`);
  console.log(TONES.map((t) => `  ${String(t).padStart(3)}: ${ramp[t]}`).join(""). replace(/(.{72})/g, "$1\n"));
}

console.log("\n=== key checks ===");
console.log("neutral  6 (dark ground)  :", out.neutral[6], " web #111400");
console.log("neutral 10 (dark card)    :", out.neutral[10], " web #1a1d00");
console.log("primary 90 (cream ink)    :", out.primary[90], " web #f6eed8");
console.log("neutral 98 (light ground) :", out.neutral[98]);

// ── Emit Color.kt ────────────────────────────────────────────────────────────
import { writeFileSync } from "node:fs";

const P = out.primary, S = out.secondary, T = out.tertiary;
const N = out.neutral, NV = out.neutralVariant, E = out.error;
const k = (h) => `Color(0xFF${h.slice(1).toUpperCase()})`;

const DARK_ROLES = [
  ["primary", P[80]], ["onPrimary", P[20]], ["primaryContainer", P[30]], ["onPrimaryContainer", P[90]],
  ["inversePrimary", P[40]],
  ["secondary", S[80]], ["onSecondary", S[20]], ["secondaryContainer", S[30]], ["onSecondaryContainer", S[90]],
  ["tertiary", T[80]], ["onTertiary", T[20]], ["tertiaryContainer", T[30]], ["onTertiaryContainer", T[90]],
  ["background", N[6]], ["onBackground", N[90]],
  ["surface", N[6]], ["onSurface", N[90]],
  ["surfaceVariant", NV[30]], ["onSurfaceVariant", NV[80]],
  ["surfaceTint", P[80]],
  ["inverseSurface", N[90]], ["inverseOnSurface", N[20]],
  ["error", E[80]], ["onError", E[20]], ["errorContainer", E[30]], ["onErrorContainer", E[90]],
  ["outline", NV[60]], ["outlineVariant", NV[30]], ["scrim", N[0]],
  ["surfaceBright", N[24]], ["surfaceDim", N[6]],
  ["surfaceContainerLowest", N[4]], ["surfaceContainerLow", N[10]], ["surfaceContainer", N[12]],
  ["surfaceContainerHigh", N[17]], ["surfaceContainerHighest", N[22]],
];

// Finance semantics. M3 has no slot for these; `error` stays a UI error state.
const roles = (list) => list.map(([n, h]) => `    ${n} = ${k(h)},`).join("\n");

const CHART_DARK = [P[95], P[87], P[70], S[60], S[50], NV[40], NV[30]];

writeFileSync(
  process.argv[2],
  `package com.basbasdev.cashette.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Generated from the cashetteweb palette. Ground and card tones gamut-clip onto the
// web's exact oklch values; see CLAUDE.md before changing any of it by hand.

val CashetteDarkColors = darkColorScheme(
${roles(DARK_ROLES)}
)

// Money has direction; M3 roles do not encode it. The error role stays a UI failure state.
val IncomeDark = ${k("#4ade80")}
val OnIncomeDark = ${k(N[6])}
val IncomeContainerDark = ${k(lchToHex(28, 40, 149.6))}
val OnIncomeContainerDark = ${k(lchToHex(90, 30, 149.6))}

val ExpenseDark = ${k("#ff6467")}
val OnExpenseDark = ${k(N[6])}
val ExpenseContainerDark = ${k(lchToHex(28, 45, 26.5))}
val OnExpenseContainerDark = ${k(lchToHex(90, 25, 26.5))}

// Ranked-category ramp for charts: one hue family, ordered by magnitude, never cycled.
val ChartRampDark = listOf(
${CHART_DARK.map((h) => `    ${k(h)},`).join("\n")}
)
`
);
console.log("\nwrote", process.argv[2]);
