# Dira

Phone-first Android live screen guide (Kotlin + Jetpack Compose).

Generic **Android UI coach** for arbitrary apps on a phone. No TRA / bank / PEPMIS / institution-specific playbooks.

Package: `com.dira.app` · minSdk 29 (Android 10+) · version `0.5.0-voice-precision`

## Phase status

| Phase | Status |
|-------|--------|
| 0 Blueprint | Done (docs in tanzania-guide) |
| 1 Skeleton | Done — Consent → Home → Guide, Watching/Stop, pointer |
| 2 Live loop | Done — MediaProjection, in-memory frames, mock/API client, overlay, 5‑min timeout |
| OpenRouter trial | Done — `guide-server/` vision proxy + debug APK workflow |
| **Overlay bubble** | **This tree** — draw-over-apps bubble, voice in + spoken out, precision box on the real app |
| Institution modules | Out of scope — do not add TRA/bank/PEPMIS scripts |

## Phone trial (sideload)

1. **Run the guide-server** on a machine the phone can reach (same Wi‑Fi, or an HTTPS tunnel). The OpenRouter key stays here — never in the APK.

```bash
cd guide-server
export OPENROUTER_API_KEY=sk-or-...          # from https://openrouter.ai/keys
export OPENROUTER_MODEL=inclusionai/ling-3.0-flash-vl:free   # optional; default is this free VL model
node server.mjs
# listens on http://0.0.0.0:8787
```

2. **Build or download a debug APK** (see CI below). Install it:

   - Copy `dira-debug.apk` to the phone (Drive, USB, or the GitHub release).
   - Settings → allow install from that source → open the APK → Install.
   - Android 10+ (API 29). Unknown-sources / “Install unknown apps” must be enabled for the installer app.

3. **Point the app at the server** (Home screen **Guide server URL**), for example:

   - `http://192.168.1.23:8787` on LAN (debug APK allows cleartext HTTP)
   - `https://your-tunnel.example` if you expose the server with TLS

   Leave the field blank to use the **mock** fallback (no network).

If the phone shows **Cloudflare 502**, the tunnel in front of `guide-server/` is down or overloaded — not an APK bug. Wait ~60s, restart the tunnel, tap the bubble again. Dira retries 502s once and shows a short message instead of the raw JSON.

### Overlay bubble (draw over other apps)

4. Consent → **Help me on this screen** → allow **Display over other apps** → optional mic → screen capture → Dira sends you back to the home screen with a **D bubble**. Open Gmail, WhatsApp, Chrome, YouTube, Settings, or any app → tap the bubble → type or **Voice** → **Guide step**. A **tight highlight** is drawn on that control and Dira **speaks** the step. **Hear again** repeats it. Stop watching wipes the in-memory frame.

If overlay permission is denied, Dira falls back to the in-app guide screen.

The trial APK does **not** register an Accessibility service. Play Protect often **hard-blocks** sideloaded apps that combine overlay + Accessibility + screen capture (it looks like banking malware). Guidance still uses the live screenshot.

### Bake `GUIDE_API_BASE` into a local APK

```bash
./gradlew :app:assembleDebug \
  -PUSE_MOCK_GUIDE=false \
  -PGUIDE_API_BASE=https://your-guide.example.com
```

`USE_MOCK_GUIDE=true` always uses heuristics. Empty `GUIDE_API_BASE` (and empty in-app URL) also falls back to mock.

Expected API: `POST {GUIDE_API_BASE}/v1/guide`  
JSON body `{ question, moduleId, language, imageBase64?, sanitizeNote? }`  
→ `{ instructionEn, instructionSw, spokenEn, spokenSw, appGuess, targetLabel, pointX, pointY, boxX, boxY, boxW, boxH, confidence, done? }`  
`pointX` / `pointY` are the tap center; `boxX`/`boxY`/`boxW`/`boxH` is a tight 0–1 box around that control (top-left origin). Restart `guide-server` after pulling prompt changes so the phone trial uses the new schema.

## OpenRouter

| Env | Default | Notes |
|-----|---------|--------|
| `OPENROUTER_API_KEY` | (required) | Server only |
| `OPENROUTER_MODEL` | `inclusionai/ling-3.0-flash-vl:free` | Free multimodal slug; change if OpenRouter retires it |
| `OPENROUTER_BASE_URL` | `https://openrouter.ai/api/v1/chat/completions` | Chat Completions |
| `PORT` / `HOST` | `8787` / `0.0.0.0` | Bind address |

The model is prompted as a **generic Android UI coach** with Material chrome + landmarks for popular apps (Gmail, WhatsApp, Chrome, YouTube, Instagram, Facebook, Telegram, TikTok, Settings, Play Store, Phone, Messages, Maps, Photos, Camera, Clock, Calendar, Drive, Docs, Meet, Slack, X, LinkedIn, Spotify, Netflix, Amazon). One next tap, spoken EN/SW, a tight bounding box, coordinates 0–1. It is instructed not to emit institution-specific playbooks.

## Privacy

- Capture is user-triggered only (Help + system dialogs)
- Frames: memory only — downscaled for the in-flight POST, then dropped
- Overlay HUD is not stored; it is torn down on Stop / timeout
- The sideload trial APK does **not** include Accessibility (Play Protect blocks that combo)
- Guide-server does **not** write screenshots to disk and does not log `imageBase64` or `uiTree`
- 5‑minute session timeout → same wipe as Stop
- No contacts, SMS, or storage permissions
- Debug APK may use HTTP to a LAN server; release builds keep `usesCleartextTraffic=false`

## GitHub Actions debug APK

Workflow: `.github/workflows/android-debug-apk.yml`

- Runs on `ubuntu-24.04` using the image’s preinstalled Android SDK (compileSdk 35). Does **not** use `android-actions/setup-android@v3` (that action still installs the removed `tools` package).
- Builds `dira-debug.apk` on PRs, `main`, tags `v*`, and `workflow_dispatch`
- Uploads a **workflow artifact** named `dira-debug-apk`
- On PRs: attaches the APK to a prerelease `apk-pr-<n>` and comments the download URL
- On `main` / tags: updates the `trial-apk` prerelease

Optional repo secret `GUIDE_API_BASE` bakes a public HTTPS origin into CI APKs. **Never** add `OPENROUTER_API_KEY` as an Android/Gradle secret.

## Layout

```
guide-server/          Node OpenRouter proxy (POST /v1/guide)
app/src/main/java/com/dira/app/
  capture/             SessionFrameBuffer, SanitizeStub, ScreenCaptureService
  overlay/             Bubble + pointer HUD over other apps
  a11y/                Read-only UI tree (optional Accessibility)
  guide/               GuideApiClient, MockGuideClient, HttpGuideClient
  session/             GuideSessionViewModel (timeout + wipe)
  modules/             generic Android pack
  ui/                  Consent, Home, Guide fallback
```
