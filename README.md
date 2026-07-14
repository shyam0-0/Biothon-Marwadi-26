# MedFusion AI

An AI-powered healthcare operating system that guides a patient through their entire
journey — symptom triage, report upload, AI fusion analysis, explainable results,
appointment booking, doctor consultation, and adaptive follow-up care.

Native Android · Kotlin · Jetpack Compose · MVVM + Clean Architecture · Firebase · Retrofit (FastAPI backend).

---

## Architecture

```
Presentation (Compose screens)
        ↓ observes StateFlow
ViewModels
        ↓ calls
Repository (interfaces in domain/, impl in data/)
        ↓
Remote (Retrofit → FastAPI)  +  Firebase (Auth / Firestore / Storage)
        ↓
Domain models
```

- **domain/** — framework-free models, repository interfaces, and use-case logic.
- **data/** — repository implementations, Retrofit services, DTOs, Firebase sources, mappers.
- **ui/** — Compose screens and the reusable design system (`ui/components`, `ui/theme`).
- **viewmodel/** — feature ViewModels exposing `StateFlow<UiState<…>>`.
- **navigation/** — routes and the single `NavHost`.
- **core/** — cross-cutting utilities (`Resource`, `AppError`, error mapping, `UiState`).
- **di/** — Hilt modules.

## Design system

A single visual identity lives in `ui/theme` (color, type, shape, spacing) and
`ui/components` (buttons, cards, chips/badges, text fields, scaffold, state views,
banners). New screens compose these — they never re-style primitives.

## Getting started

1. Open the project root in **Android Studio** (Ladybug or newer).
2. Create a Firebase project, add an Android app with package `com.medfusion.ai`,
   and replace `app/google-services.json` with the downloaded file. Enable
   **Authentication (Email/Password)**, **Firestore**, and **Storage**.
3. Run the FastAPI backend (see `/analyze`, `/triage`, etc. in the build plan).
   The debug build points at `http://10.0.2.2:8000/` (host loopback from the emulator).
   Screens ship with mock fallbacks so the UI runs before the backend is ready.
4. Build & run on an emulator or device (min SDK 26).

## Build phases

Implemented incrementally per `plan.md` (phases 0–12). See `PROGRESS.md` for status.
