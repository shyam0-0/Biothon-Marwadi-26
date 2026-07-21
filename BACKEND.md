Phase 7.1 — Firestore Backend Integration

The application is currently feature complete and compiling clean.

This phase is ONLY about replacing local/in-memory persistence with Firebase Firestore while preserving the existing architecture.

IMPORTANT

This is NOT a redesign.

This is NOT a refactor.

This is NOT a feature expansion.

Everything currently working must continue working exactly as it does now.

The objective is simply to make user data persist in Firestore.

──────────────────────────────

STRICTLY PRESERVE

Do NOT modify:

• UI
• Navigation
• Material 3 design
• Screen layouts
• ViewModels
• Repository interfaces
• Domain models
• Business logic
• AI workflow
• Gemini integration
• Body localization
• Notification logic
• Doctor workflow
• Patient workflow
• Demo Mode
• Dependency Injection structure
• Existing localization
• Existing animations
• Existing validation

Only replace repository implementations where persistence currently exists.

──────────────────────────────

DEMO MODE

Demo Mode is extremely important.

Do NOT remove FakeRepositories.

Do NOT delete demo data.

Instead:

DEMO_MODE = true
→ Fake repositories

DEMO_MODE = false
→ Firestore repositories

Dependency Injection should decide which implementation is used.

The app must continue functioning completely offline in Demo Mode.

──────────────────────────────

AUTHENTICATION

Use the existing Firebase Authentication already configured.

Store every patient's data under their authenticated UID.

Store every doctor's data under their authenticated UID.

Never mix patient data.

Never mix doctor data.

All Firestore access must be scoped to the current authenticated user unless intentionally shared (appointments, doctor directory, etc.).

──────────────────────────────

Implement Firestore repositories for:

✓ Patient Profile

✓ Doctor Profile

✓ Patient Passport

✓ Appointment Repository

✓ Consultation History

✓ Care Plans

✓ Prescriptions

✓ AI Consultation History

✓ Timeline

✓ Notification Repository

Only repositories that currently persist information should be migrated.

──────────────────────────────

FIRESTORE STRUCTURE

Design a clean Firestore schema that follows the existing domain models.

Prefer predictable collections.

Example only:

patients/
doctors/
appointments/
consultations/
notifications/

Use subcollections only where they improve organization.

Keep the schema scalable.

──────────────────────────────

REAL-TIME

Use Firestore snapshot listeners wherever the UI already expects reactive updates.

The UI should update automatically when data changes.

Avoid manual refreshes whenever possible.

──────────────────────────────

ERROR HANDLING

Gracefully handle:

• no internet
• permission failures
• missing documents
• empty collections
• cancelled operations

Never crash.

Reuse existing loading/error states wherever possible.

──────────────────────────────

PERFORMANCE

Avoid unnecessary reads.

Batch writes where appropriate.

Use suspend functions and Kotlin Coroutines.

Avoid duplicate listeners.

Dispose listeners correctly.

──────────────────────────────

DO NOT

Do NOT redesign architecture.

Do NOT rename models.

Do NOT rename repositories.

Do NOT change ViewModels.

Do NOT modify Gemini prompts.

Do NOT modify BodyMap.

Do NOT change Notifications.

Do NOT modify existing screens unless absolutely required for Firestore integration.

Do NOT introduce unnecessary dependencies.

──────────────────────────────

AFTER IMPLEMENTATION

Verify:

✓ project compiles

✓ Demo Mode still works

✓ Firestore mode works

✓ repositories switch correctly

✓ no compile errors

✓ no architecture regressions

Stop after this phase.

Do not begin ESP32 integration.

Do not begin Cloud Functions.

Do not begin FCM.

Do not begin Storage.

Do not start any future phases.