Phase 7.4 — Live Firestore Vitals Integration

IMPORTANT

Phase 7.3 backend is COMPLETE and working.

Do NOT modify the backend.

Do NOT modify the ESP32 code.

Do NOT redesign any architecture.

The objective of this phase is ONLY to make the Android application react to live Firestore vitals updates.

====================================================
CURRENT ARCHITECTURE
====================================================

ESP32
    ↓
FastAPI Backend
    ↓
Firestore
    ↓
Android App

The backend now writes:

patients/{patientId}/vitals/{autoId}

and

patients/{patientId}/latestVitals/current

Do NOT change this schema.

====================================================
OBJECTIVE
====================================================

Connect the Android application to:

patients/{patientId}/latestVitals/current

using the existing architecture.

The application should automatically update whenever the Firestore document changes.

====================================================
REQUIREMENTS
====================================================

Use the existing repository pattern.

Use existing ViewModels.

Use existing DI (Hilt).

Do NOT bypass architecture.

Preserve Demo Mode exactly as before.

When BuildConfig.DEMO_MODE == true

continue using Fake repositories.

When false

use Firestore.

====================================================
PATIENT DASHBOARD
====================================================

Display a small Live Vitals card.

Show:

• Heart Rate
• SpO₂
• Last Updated time

If no vitals exist yet:

Display

"Waiting for sensor..."

instead of an error.

The card should automatically update whenever Firestore changes.

No manual refresh.

====================================================
DOCTOR DASHBOARD
====================================================

If the doctor is viewing the patient,

show the latest Heart Rate and SpO₂.

Reuse the same repository/model where possible.

Do not duplicate logic.

====================================================
LOADING
====================================================

Loading:

"Waiting for live vitals..."

Empty:

"No vitals received yet."

Offline:

Use the existing offline handling already implemented.

====================================================
ERROR HANDLING
====================================================

If Firestore fails,

reuse the existing localized error UI.

Do not crash.

====================================================
DO NOT
====================================================

Do NOT modify FastAPI.

Do NOT modify ESP32.

Do NOT modify Firestore collections.

Do NOT redesign repositories.

Do NOT modify Gemini.

Do NOT modify Notifications.

Do NOT redesign Patient Dashboard.

Simply integrate the new Live Vitals card naturally into the existing UI.

====================================================
SUCCESS CRITERIA
====================================================

When the document

patients/{patientId}/latestVitals/current

changes,

the Android app automatically updates within a few seconds.

No refresh button.

No app restart.

No navigation required.

The patient and doctor should immediately see the latest heart rate and SpO₂ values.

Keep the implementation clean, minimal, and consistent with the existing MedFusion architecture.