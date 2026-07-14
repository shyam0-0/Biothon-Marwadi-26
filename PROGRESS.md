# Build Progress

Tracking implementation against `plan.md` (phases 0–12).

| Phase | Feature | Status |
|------:|---------|--------|
| 0 | Project setup, DI, design system, theme, navigation skeleton | ✅ Done |
| 1 | Patient/Doctor login + registration (Firebase Auth, role routing) | ✅ Done |
| 2 | Symptom triage (`/triage`, cases doc) | ✅ Done |
| 3 | Upload X-ray / lab report (Firebase Storage) | ✅ Done |
| 4 | Fusion analysis (`/analyze`) | ✅ Done |
| 5 | Explainable result + confidence | ✅ Done |
| 6 | PDF report download | ✅ Done |
| 7 | Appointment booking + smart queue | ✅ Done |
| 8 | Doctor dashboard + AI pre-read | ✅ Done |
| 9 | Video consultation | ✅ Done |
| 10 | Adaptive care companion | ✅ Done |
| 11 | Heartbeat anomaly + hospital alert | ✅ Done |
| 12 | Multilingual support (en/hi/ta) | ✅ Done |

**All phases (0–12) implemented.** See the feature notes below.

## Running it

### Demo Mode (no setup — just press Run) ✅
Debug builds ship with `DEMO_MODE = true`: every repository is replaced by an
in-memory fake (seeded with sample data, powered by the mock AI engine), so the
**entire app runs with no Firebase and no backend**. Just build & run on an emulator.
- Log in with **any** email/password. Patient sign-in → patient app; Doctor sign-in → doctor app.
- The doctor dashboard is pre-seeded with one patient request (with AI pre-read).
- Triage, upload, analysis, result, PDF download, booking, video permissions, care
  companion, and the vitals emergency flow all work end-to-end against the fakes.

### Going live (real Firebase + backend)
- Set `DEMO_MODE = false` in the debug block of `app/build.gradle.kts`.
- Replace `app/google-services.json` with a real one (Firebase project, package
  `com.medfusion.ai`); enable Email/Password Auth, Firestore, Storage.
- Point `API_BASE_URL` at your FastAPI backend (or keep `USE_MOCK_AI_FALLBACK` on).
- Let Android Studio generate the Gradle wrapper jar (or run `gradle wrapper`).

## Phase 0 — delivered
- Gradle (version catalog, root + app build scripts, KSP, Hilt, google-services).
- `AndroidManifest`, resources (strings/theme/colors, adaptive icon), backup rules.
- Clean-architecture package skeleton: `core/ data/ domain/ ui/ viewmodel/ navigation/ di/`.
- Core utilities: `Resource`, `AppError` + `ErrorMapper`, `UiState`, dispatcher DI.
- Design system: theme (light/dark + semantic colors), typography, shapes, spacing;
  components — buttons, cards, chips (urgency/confidence), text fields, scaffold,
  state views (loading/empty/error/retry), info banner, brand logo.
- Navigation: `Routes` map for the whole app + `MedFusionNavHost` wiring
  `role_selection → patient_login / doctor_login`.
- `MainActivity` + `MedFusionApplication` (Hilt).
