# MedFusion AI — Android Studio Build Prompts

Use these prompts one at a time with your AI coding assistant (Claude Code, or paste into Claude chat and copy the code into Android Studio). Built for Kotlin + Jetpack Compose + Firebase + Retrofit (FastAPI backend). Do them in order — later prompts assume earlier ones exist.

---

## 0. Project Setup Prompt

```
Set up a new Android Studio project in Kotlin using Jetpack Compose.
Configure Gradle dependencies for: Firebase Authentication, Firebase Firestore,
Firebase Storage, Retrofit + Moshi (for calling a FastAPI backend), Coil (image loading),
Navigation Compose, and ViewModel/LiveData (MVVM architecture).
Set up the package structure as: ui/, data/, domain/, viewmodel/, navigation/.
Create a NavHost with two entry routes: "patient_login" and "doctor_login".
```

---

## 1. Patient/Doctor Login

```
Build a Jetpack Compose login screen with email and password fields, using
Firebase Authentication. After successful login, fetch the user's role
("patient" or "doctor") from a Firestore "users" collection and navigate to
"patient_dashboard" or "doctor_dashboard" accordingly.
Include a registration screen that creates the Firestore user doc with the
selected role at signup.
Use a LoginViewModel with StateFlow for loading/error/success states.
```

---

## 2. Symptom Triage (symptoms only — no upload yet)

```
Create a Retrofit service interface "MedFusionApi" with an endpoint:
POST /triage — sends { symptomsText } and returns { recommendedTest, urgencyLevel, caseId }

Build a Compose "SymptomTriageScreen" with only a multiline text field for
symptom description and a "Get Recommendation" button. No file upload on
this screen. On submit, call /triage via a TriageViewModel (StateFlow for
loading/error/success).

Save a Firestore document in a "cases" collection with fields: caseId, userId,
symptomsText, recommendedTest, urgencyLevel, status="awaiting_test", createdAt.

Display the result in a Compose Card: recommended test type + urgency level,
color-coded (green/yellow/red), with a message like "Please get this test
done, then upload your results in the next step." Include a button
"I have my results — Upload now" that navigates to the upload screen,
passing the caseId.
```

---

## 3. Upload X-ray / Lab Report (after the test is actually done)

```
Build a Compose "UploadResultsScreen" that receives a caseId as a navigation
argument. Include:
- An image picker (use ActivityResultContracts.GetContent) for X-ray/scan upload
- A file picker for lab report (PDF or image)
Upload selected files to Firebase Storage under path "reports/{userId}/{caseId}/filename".
Update the existing Firestore "cases" document (matched by caseId) — do not
create a new document — adding fields: xrayUrl, labReportUrl, status="ready_for_analysis".
Show upload progress and a success confirmation, then automatically trigger
the analysis call from the next step.
```

---

## 4. Fusion Model — Analysis (calls your FastAPI backend)

```
Add a second endpoint to MedFusionApi:
POST /analyze — sends { caseId, xrayUrl, labReportUrl, symptomsText } and returns
{ findings, confidenceLevel, riskScore }

The symptomsText is pulled from the same "cases" document created in step 2 —
fetch it by caseId rather than asking the patient to re-enter it.

Build an AnalysisViewModel that calls /analyze once upload completes (step 3),
shows a loading state, and stores the returned result back into the same
"cases" document under a "fusionResult" field, with status="analyzed".
Handle loading and error states with a sealed UiState class.
```

---

## 5. Explainable Result + Confidence Score

```
Build a Compose "ResultScreen" that displays the fusionResult from Firestore:
- Findings in plain language (large text)
- Confidence level shown as a labeled chip (Low/Moderate/High) with color coding
- A fixed disclaimer text: "This is an AI-assisted insight, not a diagnosis.
  Please consult your assigned doctor."
- A "Book Appointment" button that navigates to the booking screen, passing
  the urgencyLevel as a navigation argument
```

---

## 6. PDF Report Download

```
Add a "Download Report" button on ResultScreen. On click, call a backend
endpoint POST /generate-pdf with the report ID, which returns a PDF file URL
from Firebase Storage. Use Android's DownloadManager to download the file
to the device's Downloads folder, and show a system notification when complete.
Request WRITE_EXTERNAL_STORAGE / scoped storage permissions as needed for the
target Android SDK version.
```

---

## 7. Appointment Booking + Smart Queue

```
Build a Compose "BookAppointmentScreen" with:
- A date picker (DatePickerDialog wrapped for Compose)
- A time slot picker showing available slots (fetched from Firestore
  "doctor_availability" collection)
- A message text field to the doctor
On submit, save to Firestore "appointments" collection with fields:
patientId, doctorId, date, timeSlot, message, urgencyLevel (from step 2's cases doc),
status="pending".

On the doctor side, build a query that fetches appointments sorted first by
urgencyLevel (red > yellow > green) and then by requested date/time, so
high-urgency patients appear first in the doctor's queue.
```

---

## 8. Doctor Dashboard + AI Pre-Read

```
Build a Compose "DoctorDashboardScreen" showing a LazyColumn of appointment
requests (sorted by urgency, from step 7). Each list item expands to show:
- Patient's fusionResult summary (from step 4) at the top
- X-ray and lab report thumbnails (using Coil)
- Accept / Reschedule buttons that update the appointment's status field in
  Firestore and, if rescheduled, open a date/time picker to propose a new slot
Include a badge showing urgency level color on each list item.
```

---

## 9. Video Consultation

```
Integrate Daily.co Android SDK (or Twilio Video Android SDK) for video calls.
Add a backend endpoint POST /create-room that returns a room URL when an
appointment is accepted. Store the roomUrl in the appointment document.
Build a Compose "VideoCallScreen" that loads the Daily.co call view when
either patient or doctor taps "Join Call" from their respective appointment
detail screens. Request CAMERA and RECORD_AUDIO permissions before joining.
```

---

## 10. Adaptive Care Companion (lifestyle-only, demo-safe)

```
Build a Compose "CarePlanScreen" showing the doctor-set base care plan
(medication schedule, activity goals — entered by doctor after video call,
stored in Firestore "care_plans" collection).

Add a daily check-in form (sleep hours, activity level, mood) saved to a
"daily_logs" subcollection. Write simple rule-based logic in a CarePlanViewModel:
if sleepHours < 5 or activityLevel == "low" for 2+ consecutive days, generate
a lifestyle suggestion (e.g., "Move today's walk to evening") and display it
as a Compose notification card — apply it directly to the displayed schedule.

If a rule suggests anything related to medication (dosage/timing change),
do NOT apply it automatically — instead create a document in a
"pending_doctor_approvals" collection and show "Awaiting doctor approval"
in the UI until the doctor approves it from their dashboard.
```

---

## 11. Heartbeat Anomaly Detection + Hospital Alert (confirm-gated)

```
Build a HeartRateMonitorService (or, for demo purposes, a simulated data
generator using a Slider or random walk in a DebugSensorScreen) that emits
heart rate values periodically. Implement threshold-based anomaly detection
(e.g., sustained value outside a normal range for 3+ readings).

On anomaly detected:
1. Show a full-screen alert in the app: "Unusual heart rate detected" with
   Confirm Emergency / I'm Fine buttons
2. If no response within a set countdown OR user taps Confirm Emergency,
   send an SMS to the patient's stored emergency contact (use a backend
   endpoint that wraps Twilio's SMS API) and fetch the nearest hospital
   using a static/mock list or Google Places API, then send that hospital
   an alert with the patient's last known location
Log every anomaly event to Firestore "emergency_events" collection for
the demo audit trail.
```

---

## 12. Multilingual Support

```
Integrate Android's per-app language support (AppCompatDelegate.setApplicationLocales)
with a language picker in Settings, supporting at least English, Hindi, and Tamil.
Extract all UI strings into strings.xml with corresponding values-hi and
values-ta resource folders.

For AI-generated content (triage results, explainable results from steps 2 and 5),
add a "language" parameter to the /triage and /analyze API calls so the backend
returns text already in the selected language.
```

---

## Order to run these prompts
0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12

Steps 2, 4, 6, 11 depend on backend endpoints existing (FastAPI). If your backend
isn't ready yet when you reach those prompts, ask your AI assistant to also
generate a matching mock/stub API response so the Android UI can be built and
tested in parallel.
