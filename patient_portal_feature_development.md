# MedFusion AI - Patient Portal Development Specification

Project Name:
MedFusion AI

Project Type:
AI-Powered Telemedicine & Healthcare Assistant (Android)

Current State:
The application already has a working architecture using Kotlin, Jetpack Compose, MVVM, Hilt, Navigation, Firebase, and Demo Mode.

This task is NOT about rebuilding or refactoring the application.

The architecture is already established.

The objective is to complete every feature while preserving the current project structure.

------------------------------------------------------------

GENERAL RULES

- Preserve the current MVVM architecture.
- Do NOT refactor existing working modules.
- Do NOT rename packages or classes unnecessarily.
- Reuse existing ViewModels, Repositories and Navigation.
- Only modify files related to the feature being implemented.
- If a feature requires another unfinished feature, create clean placeholders instead of redesigning architecture.
- Maintain Demo Mode compatibility.

The goal is to make the application feel like a production-ready AI healthcare platform.

------------------------------------------------------------

AI REQUIREMENT

The application should integrate Google's Gemini API (Gemini 2.5 Flash or latest available Flash model).

Create an abstraction layer:

AiService
↓
GeminiService

The rest of the application should never communicate directly with Gemini.

Every AI response should be requested in structured JSON.

Never depend on parsing plain English responses.

------------------------------------------------------------

PROJECT IMPLEMENTATION PHASES

=========================
PHASE 1
AI SYMPTOM ANALYSIS
=========================

Objective:

The Symptom Check feature should become the intelligent entry point of the application.

Current issue:

The application currently asks for symptoms and immediately redirects users toward booking appointments.

This is incorrect.

The AI consultation should happen BEFORE suggesting any appointment.

Required Flow:

User enters symptoms
↓

AI analyzes symptoms
↓

AI explains possible conditions
↓

AI suggests tests
↓

AI suggests specialist
↓

Optional upload of medical reports/X-rays
↓

AI updates analysis
↓

User decides whether consultation is required

------------------------------------------------

The AI should generate:

• Summary of symptoms

• Possible medical conditions

Each condition should include an estimated confidence percentage.

Example:

Influenza - 72%

Common Cold - 18%

COVID-19 - 8%

Other - 2%

These are NOT medical diagnoses.

Display a disclaimer explaining that only licensed doctors can diagnose diseases.

------------------------------------------------

The AI should determine severity.

Possible categories:

Low

Moderate

High

Emergency

Emergency should display clear warning messages recommending immediate medical care.

------------------------------------------------

The AI should recommend:

Recommended specialist

Recommended laboratory tests

Recommended scans

Recommended home care

Possible precautions

Red flag symptoms

------------------------------------------------

Medical uploads

The patient may upload:

Medical reports

Blood reports

Prescriptions

X-rays

Scans

All uploads are OPTIONAL.

The patient should never be forced to upload reports.

If AI determines that uploaded reports would improve confidence, simply recommend uploading them.

Example:

"A Chest X-ray may improve analysis if available."

If unavailable, continue normally.

------------------------------------------------

Report Verification

If reports are uploaded:

Gemini should summarize findings

Extract useful observations

Update disease confidence

Recommend whether further consultation is necessary

------------------------------------------------

Appointment recommendation

Appointments should NEVER open automatically.

Instead display:

Consult General Physician

Consult Pulmonologist

Consult Dermatologist

etc.

based on AI recommendation.

Only after user chooses should Appointment module open.

=========================
PHASE 2
DOCTOR CONSULTATION & APPOINTMENTS
=========================

Objective:

Transform appointments into a complete telemedicine workflow.

Flow:

Choose recommended specialist

↓

Choose available doctor

↓

Choose date

↓

Choose time

↓

Confirm booking

↓

Video consultation

↓

Doctor notes

↓

Digital prescription

↓

Doctor approval

↓

Care Plan generation

Appointments should integrate naturally with Phase 1.

The doctor should receive:

AI summary

Uploaded reports

AI recommendations

Symptom history

This reduces consultation time.

Video consultation should be modular so a provider such as Jitsi, Agora, or 100ms can be integrated later without changing business logic.

=========================
PHASE 3
CARE PLAN & RECOVERY TRACKING
=========================

The Care Plan should only exist after:

Doctor approval

OR

User explicitly accepts an AI Wellness Plan for minor conditions.

The current static Care Plan should become dynamic.

Sections:

Diagnosis

Assigned doctor

Current medications

Dosage

Medicine reminders

Recovery goals

Lifestyle recommendations

Hydration goals

Exercise recommendations

Sleep recommendations

Next follow-up

------------------------------------------------

Daily Check-in

Users should record:

Sleep duration

Mood

Activity level

Pain level

Current symptoms

Medication taken

Temperature

Additional notes

Check-ins must be stored.

Do not simply show a success message.

Store them in local database/Firebase for future retrieval.

------------------------------------------------

Progress Analysis

Gemini should analyze historical check-ins.

Generate summaries such as:

Recovery improving

Symptoms worsening

Medication compliance decreasing

Recommend follow-up if necessary.

The doctor should also be able to review this history.


=========================
PHASE 4
COMPLETE PATIENT EXPERIENCE
=========================

Integrate every module into one seamless healthcare journey.

The final experience should feel connected.

Expected patient flow:

Open app

↓

Symptom analysis

↓

AI consultation

↓

Optional report upload

↓

Updated AI analysis

↓

Doctor consultation

↓

Prescription

↓

Care Plan

↓

Daily Check-ins



↓

Recovery tracking

↓

Follow-up appointment if required

No module should feel isolated.

Every screen should communicate with related features.

------------------------------------------------------------

UI REQUIREMENTS

Maintain current design language.

Improve UX where necessary.

Avoid clutter.

Display loading states for AI requests.

Display retry options for network failures.

Show clear medical disclaimers.

Avoid making the interface look like a chatbot.

Instead present AI as a healthcare assistant.

------------------------------------------------------------

IMPLEMENTATION STRATEGY

Implement ONLY ONE PHASE at a time.

Before writing code:

1. Analyze existing implementation.

2. Identify reusable components.

3. Explain missing logic.

4. Implement minimal changes.

5. Verify the feature.

6. Stop after completing that phase.

Never begin the next phase automatically.

Wait for approval before continuing.

Do not refactor unrelated code.

Preserve existing architecture at every step.

Patient opens app
        │
        ▼
Dashboard
        │
        ▼
Start Symptom Check
        │
        ▼
Enter symptoms
        │
        ▼
Gemini analyzes symptoms
        │
        ▼
AI returns:

• Possible conditions
• Confidence %
• Severity
• Home care
• Recommended tests
• Recommended specialist
• Red flags
        │
        ▼
Patient reviews AI response
        │
        ├──────────────┐
        ▼              ▼
Upload Reports?      Skip
(Optional)            │
        │             │
        ▼             ▼
AI verifies reports   Continue
        │
        ▼
Updated AI analysis
        │
        ▼
Consult Doctor?
        │
   ┌────┴────┐
   │         │
 No         Yes
   │         │
   ▼         ▼
Save AI   Book Appointment
Summary        │
               ▼
        Video Consultation
               │
               ▼
Doctor reviews:

AI summary

Uploaded reports

Patient history


Symptoms
               │
               ▼
Doctor Diagnosis
               │
               ▼
Prescription
               │
               ▼
Care Plan Generated
               │
               ▼
Daily Check-ins
               │
               ▼

Gemini Progress Analysis
               │
               ▼
Recovery Tracking
               │
               ▼
Follow-up if required


api key for gemini: