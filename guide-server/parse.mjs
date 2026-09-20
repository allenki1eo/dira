/**
 * Extract Dira guide JSON from a vision-model reply.
 * Models may wrap JSON in markdown fences or add prose.
 */

const DEFAULT_STEP = {
  instructionEn:
    "I could not read a tap target from the model. Ask again, or try a clearer screenshot.",
  instructionSw:
    "Sikuweza kusoma sehemu ya kubonyeza. Uliza tena, au tumia skrini iliyo wazi zaidi.",
  spokenEn:
    "I could not read a tap target. Ask again, or try a clearer screenshot.",
  spokenSw:
    "Sikuweza kusoma sehemu ya kubonyeza. Uliza tena, au tumia skrini iliyo wazi zaidi.",
  pointX: 0.5,
  pointY: 0.5,
  boxX: 0.42,
  boxY: 0.465,
  boxW: 0.16,
  boxH: 0.07,
  appGuess: "",
  targetLabel: "",
  confidence: 0.2,
  done: false,
};

const DEFAULT_BOX_W = 0.16;
const DEFAULT_BOX_H = 0.07;

export function clamp01(value, fallback = 0.5) {
  const n = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(n)) return fallback;
  // Models sometimes return 0–100 percents. Values under 10 are treated as
  // overshot fractions (clamp), not 2% / 9%.
  if (n >= 10 && n <= 100) return Math.min(1, Math.max(0, n / 100));
  return Math.min(1, Math.max(0, n));
}

export function maybeClamp01(value) {
  if (value == null || value === "") return null;
  const n = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(n)) return null;
  return clamp01(n, 0);
}

export function extractJsonObject(text) {
  if (!text || typeof text !== "string") return null;
  const trimmed = text.trim();
  const fenced = trimmed.match(/```(?:json)?\s*([\s\S]*?)```/i);
  const candidate = (fenced ? fenced[1] : trimmed).trim();
  try {
    const parsed = JSON.parse(candidate);
    if (parsed && typeof parsed === "object") return parsed;
  } catch {
    // fall through to brace scan
  }
  const start = candidate.indexOf("{");
  const end = candidate.lastIndexOf("}");
  if (start >= 0 && end > start) {
    try {
      return JSON.parse(candidate.slice(start, end + 1));
    } catch {
      return null;
    }
  }
  return null;
}

function pickBox(parsed) {
  const raw = parsed.box || parsed.bbox || parsed.boundingBox;
  let boxX = maybeClamp01(parsed.boxX ?? parsed.left);
  let boxY = maybeClamp01(parsed.boxY ?? parsed.top);
  let boxW = maybeClamp01(parsed.boxW ?? parsed.boxWidth ?? parsed.width);
  let boxH = maybeClamp01(parsed.boxH ?? parsed.boxHeight ?? parsed.height);

  if (Array.isArray(raw) && raw.length >= 4) {
    boxX = maybeClamp01(raw[0]) ?? boxX;
    boxY = maybeClamp01(raw[1]) ?? boxY;
    boxW = maybeClamp01(raw[2]) ?? boxW;
    boxH = maybeClamp01(raw[3]) ?? boxH;
  } else if (raw && typeof raw === "object" && !Array.isArray(raw)) {
    boxX = maybeClamp01(raw.x ?? raw.left ?? raw.boxX) ?? boxX;
    boxY = maybeClamp01(raw.y ?? raw.top ?? raw.boxY) ?? boxY;
    boxW = maybeClamp01(raw.w ?? raw.width ?? raw.boxW) ?? boxW;
    boxH = maybeClamp01(raw.h ?? raw.height ?? raw.boxH) ?? boxH;
  }
  return { boxX, boxY, boxW, boxH };
}

function fitBox(boxX, boxY, boxW, boxH, pointX, pointY) {
  let w = boxW;
  let h = boxH;
  let x = boxX;
  let y = boxY;
  if (w == null || h == null || w < 0.02 || h < 0.015) {
    w = DEFAULT_BOX_W;
    h = DEFAULT_BOX_H;
    x = clamp01(pointX - w / 2, 0);
    y = clamp01(pointY - h / 2, 0);
  } else {
    if (x == null) x = clamp01(pointX - w / 2, 0);
    if (y == null) y = clamp01(pointY - h / 2, 0);
  }
  x = clamp01(x, 0);
  y = clamp01(y, 0);
  w = Math.min(Math.max(w, 0.02), 1 - x);
  h = Math.min(Math.max(h, 0.015), 1 - y);
  return { boxX: x, boxY: y, boxW: w, boxH: h };
}

export function toGuideResponse(modelText, language = "en") {
  const parsed = extractJsonObject(modelText) || {};
  const instructionEn =
    String(
      parsed.instructionEn ||
        parsed.instruction ||
        parsed.step ||
        parsed.text ||
        "",
    ).trim() || DEFAULT_STEP.instructionEn;
  let instructionSw = String(parsed.instructionSw || "").trim();
  if (!instructionSw) {
    instructionSw =
      language === "sw" ? instructionEn : DEFAULT_STEP.instructionSw;
  }

  const spokenEn =
    String(parsed.spokenEn || parsed.spoken || parsed.tts || parsed.speech || "")
      .trim() || instructionEn;
  let spokenSw = String(parsed.spokenSw || "").trim();
  if (!spokenSw) {
    spokenSw = language === "sw" ? spokenEn : instructionSw;
  }

  const picked = pickBox(parsed);
  let pointX = maybeClamp01(parsed.pointX ?? parsed.x);
  let pointY = maybeClamp01(parsed.pointY ?? parsed.y);
  if (
    (pointX == null || pointY == null) &&
    picked.boxX != null &&
    picked.boxY != null &&
    picked.boxW != null &&
    picked.boxH != null
  ) {
    pointX = clamp01(picked.boxX + picked.boxW / 2, 0.5);
    pointY = clamp01(picked.boxY + picked.boxH / 2, 0.5);
  }
  pointX = pointX ?? 0.5;
  pointY = pointY ?? 0.5;

  const box = fitBox(picked.boxX, picked.boxY, picked.boxW, picked.boxH, pointX, pointY);

  return {
    instructionEn,
    instructionSw,
    spokenEn,
    spokenSw,
    appGuess: String(parsed.appGuess || parsed.app || parsed.appName || "").trim(),
    targetLabel: String(
      parsed.targetLabel || parsed.label || parsed.control || "",
    ).trim(),
    pointX,
    pointY,
    boxX: box.boxX,
    boxY: box.boxY,
    boxW: box.boxW,
    boxH: box.boxH,
    confidence: maybeClamp01(parsed.confidence) ?? 0.55,
    done: Boolean(parsed.done),
  };
}

export { DEFAULT_STEP };
