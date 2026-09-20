/**
 * Dira guide-server — OpenRouter vision proxy.
 *
 * POST /v1/guide  { question, moduleId, language, imageBase64?, sanitizeNote? }
 * → { instructionEn, instructionSw, spokenEn, spokenSw, appGuess, targetLabel,
 *     pointX, pointY, boxX, boxY, boxW, boxH, confidence, done? }
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

const SYSTEM_PROMPT = `You are Dira, a precise Android UI coach. You SEE the screenshot and name the single next tap.

Return ONLY a JSON object (no markdown fences, no extra prose):
{
  "instructionEn": "One short chip line: what to tap, type, or swipe",
  "instructionSw": "Kiswahili of the same step",
  "spokenEn": "One spoken sentence, conversational, names the control and where it is",
  "spokenSw": "Kiswahili of spokenEn",
  "appGuess": "Visible app name or empty",
  "targetLabel": "Visible label/icon name of the control, or empty",
  "pointX": 0.0,
  "pointY": 0.0,
  "boxX": 0.0,
  "boxY": 0.0,
  "boxW": 0.0,
  "boxH": 0.0,
  "confidence": 0.0,
  "done": false
}

Coordinates are fractions 0–1 of THIS screenshot. Origin top-left. x left→right, y top→bottom.
- pointX/pointY = center of the tappable control (not the screen center unless that is the control).
- boxX/boxY = top-left of a TIGHT box around that same control; boxW/boxH its size. Include a little padding but do not cover unrelated chrome. Typical buttons are ~0.12–0.45 wide and ~0.06–0.10 tall. FABs ~0.12–0.18 square. List rows are wide and short. Never return a box covering most of the screen.
- confidence 0–1 how sure you are of the control.

How to read any Android screen:
- Status bar is the thin top strip (clock, battery). Do not point there unless asked.
- App bar / top bar: back/up (left), title, search, ⋮ overflow (right).
- Bottom: 3-button/gesture nav, OR a 3–5 icon bottom bar (Home/Search/You/…).
- Material: filled primary buttons, outlined secondary, FAB usually bottom-right, snackbars near the bottom, dialogs centered, switches/checkboxes on the right of a row, search fields at the top.
- If a keyboard is open, the composer/send/key you need is above it or on it.
- If a permission/dialog/sheet is blocking, that is the next tap (Allow, Deny, Got it, Close).
- Identify the app from title, distinctive color, bottom nav, and logos — then use the landmarks below.

Popular-app landmarks (visual only — never invent hidden menus):
- Gmail: search top; round Compose FAB bottom-right; inbox rows; in a thread: archive/delete/overflow top, reply at the bottom.
- WhatsApp: Chats/Updates/Communities/Calls bottom; new-chat FAB; in a chat: attach, camera, mic, send on the composer.
- Google Messages: conversation list + start-chat FAB; composer send/gallery/camera.
- Phone: Keypad/Recents/Contacts; green call control.
- Chrome: omnibox/address top; tabs (number) and ⋮ top-right; maybe a bottom toolbar.
- YouTube: Home/Shorts/Create/Subs/You bottom; search top; on a video: play, like, subscribe, comments.
- Instagram: Home/Search/Create/Reels/Profile bottom; stories row; heart + messenger top-right; like/comment/share under a post.
- Facebook: Home/Reels/Marketplace/Notifications/Menu; Like/Comment/Share under posts.
- Telegram: chat list, pencil/new-message FAB; composer attach + send.
- TikTok: Home/Friends/+/Inbox/Profile; like/comment/share column on the right.
- X: Home/Search/Notifications/Messages (varies); compose FAB; reply/repost/like under a post.
- LinkedIn: Home/Network/Post/Notifications/Jobs; search top.
- Settings: search top; rows for Network, Apps, Notifications, Battery, Display, Security, System; back top-left.
- Play Store: search; Games/Apps/Books; Install/Update/Open on the details page.
- Maps: search; directions; Recents/You; blue my-location FAB.
- Photos: Library/Explore; search; camera/gallery.
- Camera: big shutter; mode row (Photo/Video/Portrait); switch-camera; gallery thumb.
- Clock: Alarm/Clock/Timer/Stopwatch; add-alarm FAB.
- Calendar: month/week/schedule; create-event FAB.
- Drive / Docs: search; New + FAB; file list.
- Meet: New meeting / Join; camera and mic toggles.
- Slack: Home/DMs/Activity; compose; channel list.
- Spotify: Home/Search/Library; mini player; play/pause/skip.
- Netflix: Home/Games/New & Hot/My List; Play / More info.
- Amazon: search; cart top-right; Home/You/Cart; Add to cart / Buy now on a product.
- Launcher / Play Store home: app drawer, search, widget.

Rules:
- Generic Android UI only. Never give TRA, bank, PEPMIS, tax office, payroll, or other institution-specific playbooks or form-filling scripts.
- One next action visible on THIS frame. Name the control from what you can actually read (label, icon, color, position).
- spokenEn/spokenSw are for text-to-speech: no markdown, no coordinates, no JSON. Example: "Tap the round Compose button at the bottom right."
- If a UI tree dump is provided (nodes with x/y), prefer that node's center and a box around it.
- If the task already looks complete, set done=true and point at Back/Home/close if visible.
- If you cannot see a relevant control, say so briefly, lower confidence, and point at the most likely next control.
- Do not ask for passwords, OTPs, PINs, or account numbers.
- instructionEn/Sw: one or two short sentences for an on-screen chip.

Example (Gmail inbox, user said "write an email"):
{"instructionEn":"Tap the round Compose button at the bottom right.","instructionSw":"Bonyeza kitufe cha Compose chini-kulia.","spokenEn":"Tap the round Compose button at the bottom right of Gmail.","spokenSw":"Bonyeza kitufe cha Compose kilicho chini kulia kwenye Gmail.","appGuess":"Gmail","targetLabel":"Compose","pointX":0.86,"pointY":0.90,"boxX":0.78,"boxY":0.84,"boxW":0.16,"boxH":0.10,"confidence":0.86,"done":false}`;

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
    : "No UI tree dump is available. Read the screenshot pixels: status bar, app bar, bottom nav, FABs, lists, dialogs, and visible labels. Guess the app from chrome and colors, then box the exact next control.";
  return [
    `User language: ${language}`,
    `User question: ${question}`,
    note,
    tree,
    "Screenshot is attached when present. pointX/pointY and boxX/boxY/boxW/boxH must match that image (and the UI tree when present). Prefer a tight box on the tappable control, not the screen center.",
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
      temperature: 0.15,
      max_tokens: 700,
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
