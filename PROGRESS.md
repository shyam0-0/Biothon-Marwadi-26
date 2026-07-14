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

## Setup required before first run
- Replace `app/google-services.json` with a real one (Firebase project, package `com.medfusion.ai`); enable Email/Password Auth, Firestore, Storage.
- Open in Android Studio and let it generate the Gradle wrapper jar (or run `gradle wrapper`).
- Debug builds fall back to on-device mock AI when the FastAPI backend is unreachable, so the full journey is demoable without the backend.

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
