# Dira

Phone-first Android live screen guide (Kotlin + Jetpack Compose).

Specs: `../tanzania-guide/` (`PHASE0_BLUEPRINT.md`, `PRODUCT_PRIVACY_RULES.md`, `TECH_STACK.md`).

Package: `com.dira.app` · minSdk 29 (Android 10+)

## Phase status

| Phase | Status |
|-------|--------|
| 0 Blueprint | Done (docs in tanzania-guide) |
| 1 Skeleton | Done — Consent → Home → Guide, Watching/Stop, fake pointer |
| **2 Live loop** | **Done** — MediaProjection, sanitize stub, mock/API client, overlay from guide targets, 5‑min timeout + buffer wipe |
| 3 First real module | Not started (no TRA/bank hardcoding) |

## Phase 2 — what works

1. Consent (SW/EN) → Home → **Help me on this screen**
2. System **MediaProjection** permission prompt
3. Foreground service (`mediaProjection`) captures frames into an **in-memory buffer only**
4. On-device **sanitize stub** (crop + placeholder redaction bars) before any payload is built
5. **Guide API client**:
   - **Mock mode** (default): heuristics return next-step text + `pointX`/`pointY` fractions
   - **Real HTTPS** client ready for FastAPI/Node at `{GUIDE_API_BASE}/v1/guide`
6. Overlay pointer moves from guide response fractions (not hardcoded forever)
7. **Watching…** bar stays visible while capturing; notification while service runs
8. **Stop watching** ends projection, wipes buffer, shows **Session cleared**
9. **5‑minute session timeout** → same wipe + cleared banner
10. Demo module pack only — no Accessibility, contacts, SMS, or storage permissions

## Run (Android Studio)

1. Open this folder in Android Studio (Ladybug+ / AGP 8.7).
2. Run `app` on a device/emulator **API 29+**.
3. Accept consent → Help → allow screen capture → Watching → Guide step / ask → Stop.

### Mock vs API

Default (no backend needed):

```properties
# gradle.properties or command line — defaults already mock
USE_MOCK_GUIDE=true
GUIDE_API_BASE=
```

Point at a real guide backend:

```bash
./gradlew :app:assembleDebug \
  -PUSE_MOCK_GUIDE=false \
  -PGUIDE_API_BASE=https://your-guide.example.com
```

Expected API: `POST /v1/guide` JSON body `{ question, moduleId, language, imageBase64?, sanitizeNote? }` → `{ instructionEn, instructionSw, pointX, pointY, done? }`.

### Mock question hints

- blank / anything → point at first demo control  
- `submit` / `wasilisha` → submit target  
- `done` / `maliza` → finish tip  

## Privacy (Phase 2)

- Capture is user-triggered only (Help + system dialog)
- Frames: memory only — cleared on Stop / timeout / service destroy
- No screenshot DB or files retained by the app
- HTTPS only when API mode is enabled (`usesCleartextTraffic=false`)

## Layout

```
app/src/main/java/com/dira/app/
  capture/     SessionFrameBuffer, SanitizeStub, ScreenCaptureService
  guide/       GuideApiClient, MockGuideClient, HttpGuideClient
  session/     GuideSessionViewModel (timeout + wipe)
  modules/     DemoModulePack
  ui/          Consent, Home, Guide, FakePointerOverlay
```
