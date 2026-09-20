/**
 * Extract Dira guide JSON from a vision-model reply.
 * Models may wrap JSON in markdown fences or add prose.
 */

const DEFAULT_STEP = {
  instructionEn:
    "I could not read a tap target from the model. Ask again, or try a clearer screenshot.",
  instructionSw:
    "Sikuweza kusoma sehemu ya kubonyeza. Uliza tena, au tumia skrini iliyo wazi zaidi.",
  pointX: 0.5,
  pointY: 0.5,
  done: false,
};

export function clamp01(value, fallback = 0.5) {
  const n = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(n)) return fallback;
  // Models sometimes return 0–100 percents. Values under 10 are treated as
  // overshot fractions (clamp), not 2% / 9%.
  if (n >= 10 && n <= 100) return Math.min(1, Math.max(0, n / 100));
  return Math.min(1, Math.max(0, n));
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
  return {
    instructionEn,
    instructionSw,
    pointX: clamp01(parsed.pointX ?? parsed.x, 0.5),
    pointY: clamp01(parsed.pointY ?? parsed.y, 0.5),
    done: Boolean(parsed.done),
  };
}

export { DEFAULT_STEP };
