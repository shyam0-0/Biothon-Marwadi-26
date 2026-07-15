package com.medfusion.ai.data.ai

import com.medfusion.ai.domain.model.DailyLog

/** Builds the instruction prompt for symptom analysis and defines the JSON contract. */
internal object GeminiPrompt {

    private fun languageName(code: String): String = when (code.lowercase()) {
        "hi" -> "Hindi"
        "ta" -> "Tamil"
        else -> "English"
    }

    fun build(symptoms: String, language: String, hasAttachments: Boolean): String {
        val lang = languageName(language)
        val reportClause = if (hasAttachments) {
            "The patient has attached one or more medical reports/images (provided alongside this " +
                "prompt). Read them, summarize the relevant findings, and use them to refine the " +
                "condition confidences. Reflect this in the summary."
        } else {
            "No medical reports were attached. If a specific report (e.g. a chest X-ray or a blood " +
                "test) would materially improve the assessment, set \"reportRecommendation\" to a short " +
                "sentence suggesting it; otherwise leave it null."
        }

        return """
You are MedFusion AI, a careful clinical decision-support assistant. You are NOT a doctor and you
do NOT diagnose. You help a patient understand possible causes of their symptoms and what to do next.

Analyze the following patient-reported symptoms:
"$symptoms"

$reportClause

Write ALL human-readable text in $lang.

Respond with ONLY a single JSON object (no markdown, no commentary) using EXACTLY this schema:
{
  "summary": "2-3 sentence plain-language summary of the presentation",
  "conditions": [ { "name": "condition name", "confidence": 0-100 } ],  // 2-5 items, confidences should sum to ~100
  "severity": "low" | "moderate" | "high" | "emergency",
  "emergencyMessage": "if severity is emergency, a clear instruction to seek immediate care; else null",
  "recommendedSpecialists": ["specialist type, e.g. General Physician, Pulmonologist"],
  "recommendedTests": ["laboratory tests"],
  "recommendedScans": ["imaging scans, if any"],
  "homeCare": ["self-care measures"],
  "precautions": ["precautions to take"],
  "redFlags": ["warning symptoms that require urgent care"],
  "reportRecommendation": "short suggestion to upload a specific report, or null",
  "consultationRecommended": true | false
}

Rules:
- Be conservative: if symptoms could indicate a serious condition, raise the severity.
- confidence values are estimates, not diagnoses.
- Keep every list concise (max 5 items).
- Output valid JSON only.
""".trim()
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
  "followUpRecommended": true | false
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
}
