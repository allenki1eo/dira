# Dira

Phone-first Android live screen guide (Kotlin + Jetpack Compose).

Generic **Android UI coach** for arbitrary apps on a phone. No TRA / bank / PEPMIS / institution-specific playbooks.

Package: `com.dira.app` · minSdk 29 (Android 10+) · version `0.3.0-openrouter-trial`

## Phase status

| Phase | Status |
|-------|--------|
| 0 Blueprint | Done (docs in tanzania-guide) |
| 1 Skeleton | Done — Consent → Home → Guide, Watching/Stop, pointer |
| 2 Live loop | Done — MediaProjection, in-memory frames, mock/API client, overlay, 5‑min timeout |
| **OpenRouter trial** | **This tree** — `guide-server/` vision proxy + debug APK workflow |
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

4. Consent → **Help me on this screen** → allow screen capture → switch to **any** app → return to Dira (or keep the overlay in mind) → type what you want → **Guide step**. Stop watching wipes the in-memory frame.

### Bake `GUIDE_API_BASE` into a local APK

```bash
./gradlew :app:assembleDebug \
  -PUSE_MOCK_GUIDE=false \
  -PGUIDE_API_BASE=https://your-guide.example.com
```

`USE_MOCK_GUIDE=true` always uses heuristics. Empty `GUIDE_API_BASE` (and empty in-app URL) also falls back to mock.

Expected API: `POST {GUIDE_API_BASE}/v1/guide`  
JSON body `{ question, moduleId, language, imageBase64?, sanitizeNote? }`  
→ `{ instructionEn, instructionSw, pointX, pointY, done? }`  
`pointX` / `pointY` are normalized **0–1** fractions of the screenshot (top-left origin).

## OpenRouter

| Env | Default | Notes |
|-----|---------|--------|
| `OPENROUTER_API_KEY` | (required) | Server only |
| `OPENROUTER_MODEL` | `inclusionai/ling-3.0-flash-vl:free` | Free multimodal slug; change if OpenRouter retires it |
| `OPENROUTER_BASE_URL` | `https://openrouter.ai/api/v1/chat/completions` | Chat Completions |
| `PORT` / `HOST` | `8787` / `0.0.0.0` | Bind address |

The model is prompted as a **generic Android UI coach**: one next tap, both EN/SW text, coordinates 0–1. It is instructed not to emit institution-specific playbooks.

## Privacy

- Capture is user-triggered only (Help + system dialog)
- Frames: memory only — downscaled for the in-flight POST, then dropped
- Guide-server does **not** write screenshots to disk and does not log `imageBase64`
- 5‑minute session timeout → same wipe as Stop
- No Accessibility, contacts, SMS, or storage permissions
- Debug APK may use HTTP to a LAN server; release builds keep `usesCleartextTraffic=false`

## GitHub Actions debug APK

Workflow: `.github/workflows/android-debug-apk.yml`

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
  guide/               GuideApiClient, MockGuideClient, HttpGuideClient
  session/             GuideSessionViewModel (timeout + wipe)
  modules/             generic Android pack
  ui/                  Consent, Home, Guide, pointer overlay
```
