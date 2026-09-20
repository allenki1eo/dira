# Dira guide-server

Tiny Node HTTP proxy. The Android app posts a **temporary session frame** + question; this process calls OpenRouter vision and returns Dira’s existing JSON shape. **Do not put `OPENROUTER_API_KEY` in the APK.**

Default model: `inclusionai/ling-3.0-flash-vl:free` (free multimodal on OpenRouter as of 2026-09). Override with `OPENROUTER_MODEL` if that slug is retired.

## Run

```bash
cd guide-server
cp .env.example .env   # then edit
export OPENROUTER_API_KEY=sk-or-...
export OPENROUTER_MODEL=inclusionai/ling-3.0-flash-vl:free   # optional
node server.mjs
```

Listens on `http://0.0.0.0:8787` by default (`PORT`, `HOST` override).

- `GET /health` — liveness; does not echo secrets
- `POST /v1/guide` — `{ question, moduleId, language, imageBase64?, sanitizeNote? }` → `{ instructionEn, instructionSw, spokenEn, spokenSw, appGuess, targetLabel, pointX, pointY, boxX, boxY, boxW, boxH, confidence, done }`

`pointX`/`pointY` are the tap center (0–1). `box*` is a tight highlight around the same control. Restart this process after prompt changes so the phone trial picks them up.

Frames are held only for the in-flight request. They are not logged or written to disk.

## Reachable from a phone

The APK must be able to HTTP(S) to this host:

- Same Wi-Fi: `http://YOUR_LAN_IP:8787` (debug APK allows cleartext)
- Off-network: tunnel with HTTPS (ngrok, Cloudflare Tunnel, Caddy) and use that origin as `GUIDE_API_BASE`
