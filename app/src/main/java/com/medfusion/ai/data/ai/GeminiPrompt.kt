package com.medfusion.ai.data.ai

import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.PatientContext
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.domain.model.SymptomLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds the instruction prompt for symptom analysis and defines the JSON contract. */
internal object GeminiPrompt {

    private fun languageName(code: String): String = when (code.lowercase()) {
        "hi" -> "Hindi"
        "ta" -> "Tamil"
        else -> "English"
    }

    fun build(
        symptoms: String,
        language: String,
        hasAttachments: Boolean,
        context: PatientContext? = null,
        locations: List<SymptomLocation> = emptyList(),
    ): String {
        val lang = languageName(language)
        val reportClause = if (hasAttachments) {
            "The patient has attached one or more medical reports/images (provided alongside this " +
                "prompt). Read them carefully and fill \"reportInsights\": summarize the findings, " +
                "highlight every abnormal value with its measured level, list important observations, " +
                "compare the findings with the current symptoms, and state explicitly whether they " +
                "support or weaken each predicted condition. Update the condition confidences " +
                "accordingly and explain the change in \"confidenceExplanation\". Recommend further " +
                "investigations only if the findings genuinely require them. If an uploaded report is " +
                "unrelated to the symptoms, clearly explain in \"reportInsights.relevance\" why it does " +
                "not contribute to this assessment."
        } else {
            "No medical reports were attached, so set \"reportInsights\" to null. Uploads are always " +
                "optional — never pressure the patient. Only if a specific report (e.g. a chest X-ray " +
                "or a blood test) would materially change the assessment, set \"reportRecommendation\" " +
                "to a short sentence suggesting it; otherwise leave it null."
        }

        return """
You are MedFusion AI, a careful clinical decision-support assistant. You are NOT a doctor and you
do NOT diagnose. You help a patient understand possible causes of their symptoms and what to do next.

Analyze the following patient-reported symptoms:
"$symptoms"

${localizationBlock(locations)}${contextBlock(context)}
$reportClause

Write ALL human-readable text in $lang.

Respond with ONLY a single JSON object (no markdown, no commentary) using EXACTLY this schema:
{
  "summary": "2-3 sentence plain-language summary of the presentation",
  "conditions": [ { "name": "condition name", "confidence": 0-100, "reason": "short explanation of why this condition fits these symptoms" } ],  // the TOP 3 most likely, confidences should sum to ~100
  "severity": "low" | "moderate" | "high" | "emergency",
  "emergencyMessage": "if severity is emergency, a clear instruction to seek immediate care; else null",
  "recommendedSpecialists": [ { "name": "specialist type, e.g. Cardiologist", "reason": "why this specialist, tied to the predicted conditions AND the symptoms" } ],
  "recommendedTests": [ { "name": "test name", "priority": "required" | "recommended" | "optional", "reason": "what this test would confirm or rule out" } ],
  "recommendedScans": ["imaging scans, if any"],
  "homeCare": ["self-care measures"],
  "precautions": ["precautions to take"],
  "redFlags": ["warning symptoms that require urgent care"],
  "reportRecommendation": "short suggestion to upload a specific report, or null",
  "consultationRecommended": true | false,
  "confidenceExplanation": "if reports, previous consultations or history changed the confidences, explain WHY in 1-2 sentences; else null",
  "reportInsights": { "summary": "what the uploaded reports show", "abnormalValues": ["abnormal findings"], "concerns": ["possible concerns"], "relevance": "how the reports affect this assessment, or why they don't contribute" } or null
}

Rules:
- MEDICAL SAFETY FIRST. If the presentation includes any emergency indicator — severe or crushing
  chest pain, stroke signs (face droop, arm weakness, slurred speech), severe breathing difficulty,
  unconsciousness or fainting, seizures, uncontrolled bleeding, signs of anaphylaxis — set severity
  to "emergency" and write a direct "emergencyMessage" telling the patient to seek immediate medical
  attention and NOT to wait for a consultation. Never downplay a possible emergency.
- Be conservative: if symptoms could indicate a serious condition, raise the severity.
- Rank conditions by likelihood for THIS patient: weigh symptom pattern, duration, age, chronic
  diseases, current medications and previous diagnoses. Each condition's "reason" must cite the
  specific reported symptoms (and any history) that support it — never a textbook definition.
- confidence values are estimates, not diagnoses.
- Recommendations must be SPECIFIC to these symptoms and this patient — never a generic template.
  Chest pain → ECG, Troponin, chest X-ray and a Cardiologist; persistent cough → chest X-ray, CBC,
  COVID test and a Pulmonologist; abdominal pain → ultrasound, LFT, urine analysis and a
  Gastroenterologist; headache → blood pressure and eye examination first, MRI ONLY when
  neurological signs are present. Reason from the actual presentation.
- Each test's "reason" must state its clinical relevance: what it confirms or rules out for the
  suspected conditions. Mark a test "required" only when management depends on it.
- Do NOT order expensive or invasive investigations (MRI, CT, endoscopy) unless clinically
  indicated by the presentation.
- The specialist "reason" must connect BOTH the leading conditions AND the actual symptoms to that
  specialty. Factor known allergies and current medications into home care and precautions.
- Vary phrasing naturally with the presentation; two different patients must never receive
  identical text.
- Only recommend a test when it is useful for THIS presentation; do not pad the list.
- Keep every list concise (max 5 items).
- Output valid JSON only.
""".trim()
    }

    /**
     * Smart symptom localization block (Phase 5.6): the patient marked where
     * symptoms occur on a body map, with quality, severity, onset and progression.
     */
    private fun localizationBlock(locations: List<SymptomLocation>): String {
        if (locations.isEmpty()) return ""
        return """
The patient localized their symptoms on a body map:
${locations.joinToString("\n") { "- ${it.summary()}" }}

Weigh these locations, severities, durations and progressions heavily when ranking conditions,
estimating confidence, choosing the specialist and recommending investigations. Left/right side
matters (e.g. left-sided chest pain vs. right-sided), and worsening high-severity symptoms should
raise severity.
""".trim() + "\n\n"
    }

    /**
     * The Smart AI Context block (Phase 5): everything already known about the
     * patient, so the model reasons about a returning patient — not a stranger.
     */
    private fun contextBlock(context: PatientContext?): String {
        if (context == null || context.isEmpty) return ""
        val lines = mutableListOf<String>()
        val p = context.passport
        if (p != null && !p.isEmpty) {
            val profile = listOfNotNull(
                p.age?.let { "age $it" },
                p.gender.takeIf { it.isNotBlank() },
                p.bloodGroup.takeIf { it.isNotBlank() }?.let { "blood group $it" },
                p.bmi?.let { "BMI $it" },
            ).joinToString(", ")
            if (profile.isNotBlank()) lines += "- Profile: $profile"
            if (p.allergies.isNotEmpty()) lines += "- Known allergies: ${p.allergies.joinToString()}"
            if (p.chronicDiseases.isNotEmpty()) lines += "- Chronic diseases: ${p.chronicDiseases.joinToString()}"
            if (p.currentMedications.isNotEmpty()) lines += "- Current medications: ${p.currentMedications.joinToString()}"
            if (p.previousDiagnoses.isNotEmpty()) lines += "- Previous diagnoses: ${p.previousDiagnoses.joinToString()}"
            if (p.previousSurgeries.isNotEmpty()) lines += "- Previous surgeries: ${p.previousSurgeries.joinToString()}"
            val lifestyle = listOfNotNull(
                "smoker".takeIf { p.smoker },
                "consumes alcohol".takeIf { p.alcohol },
                "pregnant".takeIf { p.pregnant },
            ).joinToString(", ")
            if (lifestyle.isNotBlank()) lines += "- Lifestyle: $lifestyle"
        }
        context.latestDiagnosis?.takeIf { it.isNotBlank() }?.let {
            lines += "- Latest doctor diagnosis: $it"
        }
        context.carePlan?.let { plan ->
            val meds = plan.medications.joinToString { m -> "${m.name} ${m.dosage}" }
            lines += "- Active care plan${plan.diagnosis?.let { d -> " for $d" } ?: ""}" +
                (if (meds.isNotBlank()) " with medications: $meds" else "")
        }
        if (context.recentLogs.isNotEmpty()) {
            val logs = context.recentLogs.sortedByDescending { it.date }.take(3).joinToString("; ") { l ->
                "${l.date}: pain ${l.painLevel}/10, mood ${l.mood.wireValue}, " +
                    "medication ${if (l.medicationTaken) "taken" else "missed"}" +
                    (l.temperature?.let { t -> ", temp ${t}°F" } ?: "") +
                    (l.currentSymptoms.takeIf { s -> s.isNotBlank() }?.let { s -> ", symptoms: $s" } ?: "")
            }
            lines += "- Recent daily check-ins: $logs"
        }
        if (context.aiHistory.isNotEmpty()) {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val history = context.aiHistory.take(3).joinToString("; ") { record ->
                val top = record.conditions.maxByOrNull { it.confidence }
                "${fmt.format(Date(record.dateMillis))}: \"${record.symptoms.take(80)}\" → " +
                    (top?.let { "${it.name} (${it.confidence}%)" } ?: "no leading condition") +
                    ", severity ${record.severity.wireValue}" +
                    (record.locations.takeIf { it.isNotEmpty() }
                        ?.let { locs -> " [localized: ${locs.joinToString(" | ")}]" } ?: "")
            }
            lines += "- Previous AI consultations: $history"
        }
        if (lines.isEmpty()) return ""
        return """
Known patient background (from the Patient Passport — this is a returning patient):
${lines.joinToString("\n")}

Use this history: factor allergies and current medications into every recommendation, relate new
symptoms to previous consultations and the doctor's diagnosis, and if the patient's history changes
your confidence versus a symptoms-only assessment, explain that in "confidenceExplanation".
""".trim() + "\n"
    }

    fun buildProgress(logs: List<DailyLog>, diagnosis: String?, language: String): String {
        val lang = languageName(language)
        val rows = logs.sortedBy { it.date }.joinToString("\n") { l ->
            "date=${l.date}, sleep=${l.sleepHours}h, activity=${l.activityLevel.wireValue}, " +
                "mood=${l.mood.wireValue}, pain=${l.painLevel}/10, medTaken=${l.medicationTaken}, " +
                "temp=${l.temperature ?: "-"}, symptoms=\"${l.currentSymptoms}\""
        }
        return """
You are MedFusion AI reviewing a patient's daily recovery check-ins.
${diagnosis?.let { "Working diagnosis: $it." } ?: ""}
Check-ins (oldest first):
$rows

Assess the recovery trend. Write text in $lang.
Respond with ONLY this JSON:
{
  "status": "one short label, e.g. Recovery improving | Symptoms worsening | Medication compliance decreasing | Recovery stable",
  "summary": "2-3 sentences explaining the trend and what to do",
  "followUpRecommended": true | false,
  "followUpReason": "a clear one-sentence explanation of WHY a follow-up is or is not recommended (worsening symptoms, missed medication, rising pain or temperature, poor recovery...)"
}
Be conservative: if the trend is worsening, set followUpRecommended to true. JSON only.
""".trim()
    }

    fun buildWellness(concern: String, language: String): String {
        val lang = languageName(language)
        return """
You are MedFusion AI creating a safe self-care WELLNESS plan for a MINOR condition
(no prescription-only medicines; only common OTC options if appropriate). Write text in $lang.
Patient concern: "$concern"

Respond with ONLY this JSON:
{
  "medications": [ { "name": "", "dosage": "", "timing": "" } ],
  "activityGoals": ["daily activity goals"],
  "recoveryGoals": ["recovery goals"],
  "lifestyle": ["lifestyle recommendations"],
  "hydration": "hydration goal",
  "exercise": "exercise recommendation",
  "sleep": "sleep recommendation",
  "note": "short note reminding this is self-care and to see a doctor if it worsens"
}
Keep every list concise (max 4 items). JSON only.
""".trim()
    }

    /** Patient-friendly translation of the doctor's clinical outcome (Phase 5.6). */
    fun buildExplain(prescription: Prescription, carePlan: CarePlan?, language: String): String {
        val lang = languageName(language)
        val meds = prescription.medications.joinToString("\n") { "- ${it.name}, ${it.dosage}, ${it.timing}" }
        val planBlock = carePlan?.let { plan ->
            val lines = listOfNotNull(
                plan.recoveryGoals.takeIf { it.isNotEmpty() }?.let { "Recovery goals: ${it.joinToString()}" },
                plan.lifestyle.takeIf { it.isNotEmpty() }?.let { "Lifestyle: ${it.joinToString()}" },
                plan.hydration?.let { "Hydration: $it" },
                plan.exercise?.let { "Exercise: $it" },
                plan.sleep?.let { "Sleep: $it" },
            )
            if (lines.isEmpty()) "" else "Care plan:\n${lines.joinToString("\n")}"
        }.orEmpty()

        return """
You are MedFusion AI translating a doctor's clinical outcome into simple language a patient can
understand. You are NOT diagnosing — you are ONLY explaining what the doctor already decided.

Doctor's diagnosis: "${prescription.diagnosis}"
Doctor's advice: "${prescription.advice}"
Prescribed medicines:
${meds.ifBlank { "- none" }}
${prescription.followUpDate?.let { "Follow-up date: $it" } ?: ""}
$planBlock

Write ALL text in $lang. Respond with ONLY this JSON:
{
  "whatDoctorFound": "the diagnosis explained in 2-4 short, simple sentences — no medical jargon",
  "medicines": [ { "name": "medicine name", "purpose": "what it does, when to take it and why it matters, in plain words" } ],
  "whatToDo": ["short daily instructions: rest, hydration, food, activity restrictions, sleep..."],
  "recovery": "what improvement to expect, how long recovery usually takes, and what is normal — 2-3 sentences",
  "warningSigns": ["specific signs that mean the patient should seek medical attention"]
}

Rules:
- Short sentences. Supportive tone. Medically accurate.
- NEVER invent information, add diagnoses, contradict the doctor, or promise recovery.
- Explain only what is in the doctor's record above.
- This does not replace professional advice.
- Output valid JSON only.
""".trim()
    }
}
