/**
 * Dira guide-server — OpenRouter vision proxy.
 *
 * POST /v1/guide  { question, moduleId, language, imageBase64?, sanitizeNote? }
 * → { instructionEn, instructionSw, pointX, pointY, done? }
 *
 * Privacy: frames live only in this request. They are never written to disk
 * and are not logged. Drop the request body after the handler returns.
 */
import http from "node:http";
import { toGuideResponse } from "./parse.mjs";

const PORT = Number(process.env.PORT || 8787);
const HOST = process.env.HOST || "0.0.0.0";
const OPENROUTER_API_KEY = process.env.OPENROUTER_API_KEY || "";
const OPENROUTER_MODEL =
  process.env.OPENROUTER_MODEL || "inclusionai/ling-3.0-flash-vl:free";
const OPENROUTER_BASE_URL =
  process.env.OPENROUTER_BASE_URL ||
  "https://openrouter.ai/api/v1/chat/completions";
const MAX_BODY_BYTES = 2_500_000;

const SYSTEM_PROMPT = `You are Dira, a generic Android UI coach for ANY app on a phone.

Look at the screenshot of the user's current Android screen and tell them the single next action.

Return ONLY a JSON object (no markdown fences, no extra prose):
{
  "instructionEn": "One short next-step: what to tap, type, or swipe",
  "instructionSw": "Kiswahili translation of the same step",
  "pointX": 0.0,
  "pointY": 0.0,
  "done": false
}

Rules:
- Generic Android UI only. Never give TRA, bank, PEPMIS, tax office, payroll, or other institution-specific playbooks or form-filling scripts.
- Describe the next single tap or gesture visible on THIS screen.
- pointX and pointY are normalized fractions 0–1 of this screenshot (origin at top-left: x is left→right, y is top→bottom). Point at the control the user should use next.
- If a UI tree dump is provided (interactive nodes with x/y), prefer the center of the matching node for pointX/pointY.
- If the task already looks complete, set done=true and point at Back/Home/close if visible.
- If you cannot see a relevant control, say so briefly and point at the most likely next control.
- Do not ask for passwords, OTPs, PINs, or account numbers.
- Keep each instruction to one or two short sentences.`;

function sendJson(res, status, body) {
  const payload = JSON.stringify(body);
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(payload),
    "Cache-Control": "no-store",
  });
  res.end(payload);
}

function readBody(req, limit) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on("data", (chunk) => {
      size += chunk.length;
      if (size > limit) {
        reject(Object.assign(new Error("payload too large"), { status: 413 }));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
    req.on("error", reject);
  });
}

function buildUserPrompt(body) {
  const question = String(body.question || "").trim() || "(no question — suggest the most likely next tap)";
  const language = String(body.language || "en");
  const note = body.sanitizeNote ? `Sanitize note from the phone: ${body.sanitizeNote}` : "";
  const tree = body.uiTree
    ? `Foreground app UI tree (read-only accessibility dump; x/y are 0–1 centers):\n${String(body.uiTree).slice(0, 5000)}`
    : "";
  return [
    `User language: ${language}`,
    `User question: ${question}`,
    note,
    tree,
    "Screenshot is attached when present. Coordinates must match that image (and the UI tree when present).",
  ]
    .filter(Boolean)
    .join("\n");
}

async function callOpenRouter({ questionPrompt, imageBase64 }) {
  const content = [{ type: "text", text: questionPrompt }];
  if (imageBase64) {
    const raw = String(imageBase64).replace(/\s/g, "");
    const url = raw.startsWith("data:")
      ? raw
      : `data:image/jpeg;base64,${raw}`;
    content.push({ type: "image_url", image_url: { url } });
  }

  const res = await fetch(OPENROUTER_BASE_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${OPENROUTER_API_KEY}`,
      "Content-Type": "application/json",
      "HTTP-Referer": "https://github.com/allenki1eo/dira",
      "X-Title": "Dira guide-server",
    },
    body: JSON.stringify({
      model: OPENROUTER_MODEL,
      temperature: 0.2,
      max_tokens: 400,
      messages: [
        { role: "system", content: SYSTEM_PROMPT },
        { role: "user", content },
      ],
    }),
  });

  const text = await res.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    throw new Error(`OpenRouter returned non-JSON HTTP ${res.status}`);
  }
  if (!res.ok) {
    const msg =
      data?.error?.message ||
      data?.message ||
      text.slice(0, 240) ||
      `HTTP ${res.status}`;
    throw new Error(`OpenRouter error ${res.status}: ${msg}`);
  }
  const message = data?.choices?.[0]?.message?.content;
  if (typeof message !== "string" || !message.trim()) {
    throw new Error("OpenRouter returned an empty message");
  }
  return message;
}

async function handleGuide(req, res) {
  if (!OPENROUTER_API_KEY) {
    sendJson(res, 500, {
      error: "OPENROUTER_API_KEY is not set on the guide-server",
    });
    return;
  }

  let raw;
  try {
    raw = await readBody(req, MAX_BODY_BYTES);
  } catch (err) {
    sendJson(res, err.status || 400, { error: err.message || "Bad request" });
    return;
  }

  let body;
  try {
    body = JSON.parse(raw || "{}");
  } catch {
    sendJson(res, 400, { error: "Request body must be JSON" });
    return;
  }

  // Pull the image, then drop the original payload so it is not retained.
  const imageBase64 = body.imageBase64 || null;
  const language = body.language || "en";
  const questionPrompt = buildUserPrompt(body);
  body = null;
  raw = null;

  try {
    const modelText = await callOpenRouter({ questionPrompt, imageBase64 });
    const step = toGuideResponse(modelText, language);
    sendJson(res, 200, step);
  } catch (err) {
    console.error("[guide-server]", err.message || err);
    sendJson(res, 502, { error: err.message || "Guide request failed" });
  }
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);

  if (req.method === "GET" && (url.pathname === "/health" || url.pathname === "/")) {
    sendJson(res, 200, {
      ok: true,
      service: "dira-guide-server",
      model: OPENROUTER_MODEL,
      hasKey: Boolean(OPENROUTER_API_KEY),
    });
    return;
  }

  if (req.method === "POST" && url.pathname === "/v1/guide") {
    await handleGuide(req, res);
    return;
  }

  sendJson(res, 404, { error: "Not found" });
});

server.listen(PORT, HOST, () => {
  console.log(
    `[guide-server] listening on http://${HOST}:${PORT}  model=${OPENROUTER_MODEL}  key=${OPENROUTER_API_KEY ? "set" : "MISSING"}`,
  );
  console.log("[guide-server] frames are request-scoped only; nothing is written to disk");
});
