package com.medfusion.ai.data.ai

import com.medfusion.ai.data.remote.MockAiEngine
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CarePlanSource
import com.medfusion.ai.domain.model.ConditionProbability
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.domain.model.Mood
import com.medfusion.ai.domain.model.MedicineExplanation
import com.medfusion.ai.domain.model.PatientContext
import com.medfusion.ai.domain.model.PatientExplanation
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.domain.model.ProgressAnalysis
import com.medfusion.ai.domain.model.ReportInsights
import com.medfusion.ai.domain.model.Severity
import com.medfusion.ai.domain.model.SpecialistRecommendation
import com.medfusion.ai.domain.model.SymptomAnalysis
import com.medfusion.ai.domain.model.SymptomLocation
import com.medfusion.ai.domain.model.TestPriority
import com.medfusion.ai.domain.model.TestRecommendation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic offline stand-in for [GeminiService]. Used only when the Gemini
 * key is absent or the network fails in a demo/mock build, so the full symptom
 * flow stays demonstrable. Reuses [MockAiEngine]'s keyword heuristics; Phase 5
 * adds symptom-specific tests/specialists with reasons, patient-context awareness
 * and confidence evolution when reports are attached.
 */
@Singleton
class MockSymptomAnalysisProvider @Inject constructor(
    private val mockAiEngine: MockAiEngine,
) {
    fun analyze(
        symptoms: String,
        hasAttachments: Boolean,
        context: PatientContext? = null,
        locations: List<SymptomLocation> = emptyList(),
    ): SymptomAnalysis {
        // Body-map localization (Phase 5.6) feeds the keyword matcher, so
        // "Chest" + "Pain" selects the chest-pain profile even if the free text
        // only says "I have pain".
        val text = (symptoms + " " +
            locations.joinToString(" ") { "${it.region.label} ${it.descriptor}" }).lowercase()
        // Medical safety (Task 6): emergency indicators always escalate — the AI
        // must never downplay a possible emergency, even offline.
        val isEmergency = EMERGENCY_KEYWORDS.any { text.contains(it) }
        val triage = mockAiEngine.triage(symptoms, caseId = "mock")
        // Worsening high-severity localized symptoms raise the urgency.
        val worseningSevere = locations.any { it.severity >= 8 && it.progression.equals("Worse", true) }
        val severity = when {
            isEmergency -> Severity.EMERGENCY
            triage.urgencyLevel == "red" || worseningSevere -> Severity.HIGH
            triage.urgencyLevel == "yellow" -> Severity.MODERATE
            else -> Severity.LOW
        }
        val profile = profileFor(text)

        // Confidence evolution (Phase 5): attached reports raise the leading
        // condition's confidence; prior AI history nudges it too.
        val hasHistory = context?.aiHistory?.isNotEmpty() == true
        val boost = (if (hasAttachments) 12 else 0) + (if (hasHistory) 4 else 0)
        val conditions = profile.conditions.mapIndexed { index, condition ->
            if (index == 0) condition.copy(confidence = (condition.confidence + boost).coerceAtMost(95))
            else condition
        }

        val contextNotes = buildList {
            if (locations.isNotEmpty()) {
                add("The locations you marked (${locations.joinToString { it.region.label }}) were used to focus this assessment.")
            }
            context?.passport?.allergies?.takeIf { it.isNotEmpty() }?.let {
                add("Your known allergies (${it.joinToString()}) were considered in these recommendations.")
            }
            context?.latestDiagnosis?.takeIf { it.isNotBlank() }?.let {
                add("Your previous diagnosis of $it was taken into account.")
            }
        }
        val summarySuffix = buildString {
            if (hasAttachments) append(" Uploaded reports were reviewed and factored in.")
            if (contextNotes.isNotEmpty()) append(" " + contextNotes.joinToString(" "))
        }

        return SymptomAnalysis(
            summary = "Based on your description, your symptoms are most consistent with " +
                "${conditions.first().name.lowercase()}.$summarySuffix This is preliminary guidance, not a diagnosis.",
            conditions = conditions,
            severity = severity,
            emergencyMessage = if (severity == Severity.EMERGENCY)
                "Your symptoms may indicate a medical emergency. Seek immediate medical attention — " +
                    "call emergency services or go to the nearest hospital now. Do NOT wait for a consultation."
            else null,
            recommendedSpecialists = profile.specialists,
            recommendedTests = profile.tests,
            recommendedScans = profile.scans,
            homeCare = listOf("Rest and stay hydrated", "Monitor your temperature"),
            precautions = listOf("Avoid strenuous activity", "Isolate if you have a fever"),
            redFlags = profile.redFlags,
            reportRecommendation = if (!hasAttachments && !isEmergency) profile.reportSuggestion else null,
            consultationRecommended = severity != Severity.LOW,
            confidenceExplanation = when {
                hasAttachments && hasHistory ->
                    "Confidence increased because the uploaded report supports the leading condition and " +
                        "it is consistent with your previous consultations."
                hasAttachments ->
                    "Confidence in ${conditions.first().name} increased after reviewing the uploaded report."
                hasHistory ->
                    "Your previous AI consultations were factored into these estimates."
                else -> null
            },
            reportInsights = if (hasAttachments) ReportInsights(
                summary = "The uploaded report was reviewed and its findings are consistent with the leading condition.",
                abnormalValues = listOf("Mildly elevated inflammatory markers"),
                concerns = listOf("Findings warrant clinical correlation"),
                relevance = "The report supports the current assessment and increased its confidence.",
            ) else null,
        )
    }

    private companion object {
        /** Presentations that must always be treated as emergencies (Task 6). */
        val EMERGENCY_KEYWORDS = listOf(
            "severe chest pain", "crushing chest", "can't breathe", "cannot breathe",
            "severe breathing", "difficulty breathing severe", "unconscious", "fainted",
            "passed out", "seizure", "convulsion", "stroke", "slurred speech",
            "face drooping", "arm weakness", "severe bleeding", "uncontrolled bleeding",
            "suicid", "overdose", "anaphyla", "swollen throat",
        )
    }

    /** Symptom-specific bundle so recommendations are never a generic default list. */
    private data class SymptomProfile(
        val conditions: List<ConditionProbability>,
        val specialists: List<SpecialistRecommendation>,
        val tests: List<TestRecommendation>,
        val scans: List<String>,
        val redFlags: List<String>,
        val reportSuggestion: String?,
    )

    private fun profileFor(text: String): SymptomProfile = when {
        text.contains("chest pain") || (text.contains("chest") && text.contains("pain")) -> SymptomProfile(
            conditions = listOf(
                ConditionProbability("Musculoskeletal chest pain", 48, "Localized pain that changes with movement is often muscular."),
                ConditionProbability("Angina", 32, "Chest pain can indicate reduced blood flow to the heart and must be ruled out."),
                ConditionProbability("Gastro-esophageal reflux", 20, "Acid reflux commonly causes burning chest discomfort."),
            ),
            specialists = listOf(
                SpecialistRecommendation("Cardiologist", "Chest pain requires cardiac evaluation to rule out heart conditions."),
                SpecialistRecommendation("General Physician", "For initial assessment and referral if cardiac causes are excluded."),
            ),
            tests = listOf(
                TestRecommendation("ECG", TestPriority.REQUIRED, "To detect cardiac rhythm or ischemic changes."),
                TestRecommendation("Troponin", TestPriority.REQUIRED, "To rule out heart muscle injury."),
                TestRecommendation("Echocardiogram", TestPriority.OPTIONAL, "If the ECG or troponin results are abnormal."),
            ),
            scans = listOf("Chest X-ray"),
            redFlags = listOf("Pain spreading to arm or jaw", "Shortness of breath", "Sweating with nausea"),
            reportSuggestion = "A recent ECG or chest X-ray may improve the analysis if available.",
        )
        text.contains("cough") || text.contains("breath") -> SymptomProfile(
            conditions = listOf(
                ConditionProbability("Acute bronchitis", 54, "A persistent cough after a cold most often reflects airway inflammation."),
                ConditionProbability("Common cold", 26, "Viral upper-respiratory infections frequently cause cough and congestion."),
                ConditionProbability("Pneumonia", 14, "A worsening productive cough with fever can indicate a chest infection."),
            ),
            specialists = listOf(
                SpecialistRecommendation("Pulmonologist", "A persistent cough or breathing difficulty is best evaluated by a lung specialist."),
                SpecialistRecommendation("General Physician", "For initial evaluation of common respiratory infections."),
            ),
            tests = listOf(
                TestRecommendation("Chest X-ray", TestPriority.REQUIRED, "To look for infection or other lung changes."),
                TestRecommendation("Complete Blood Count (CBC)", TestPriority.RECOMMENDED, "To identify signs of infection."),
                TestRecommendation("COVID Test", TestPriority.RECOMMENDED, "Respiratory symptoms overlap with COVID-19."),
                TestRecommendation("Sputum Test", TestPriority.OPTIONAL, "If the cough is productive, to identify the organism."),
            ),
            scans = listOf("Chest X-ray"),
            redFlags = listOf("Difficulty breathing", "Chest pain", "Coughing up blood"),
            reportSuggestion = "A chest X-ray may improve the analysis if available.",
        )
        text.contains("rash") || text.contains("skin") || text.contains("itch") -> SymptomProfile(
            conditions = listOf(
                ConditionProbability("Contact dermatitis", 50, "Localized itchy rashes commonly follow contact with an irritant or allergen."),
                ConditionProbability("Fungal infection", 28, "Persistent itchy patches, especially in skin folds, suggest a fungal cause."),
                ConditionProbability("Eczema", 22, "Recurring dry itchy skin is characteristic of eczema."),
            ),
            specialists = listOf(
                SpecialistRecommendation("Dermatologist", "Skin rashes are best diagnosed visually by a skin specialist."),
            ),
            tests = listOf(
                TestRecommendation("Skin examination", TestPriority.REQUIRED, "Visual inspection usually identifies the rash type."),
                TestRecommendation("Allergy panel", TestPriority.OPTIONAL, "If an allergic trigger is suspected."),
            ),
            scans = emptyList(),
            redFlags = listOf("Rapidly spreading rash", "Fever with rash", "Blistering or peeling skin"),
            reportSuggestion = "A clear photo of the affected skin may improve the analysis.",
        )
        text.contains("stomach") || text.contains("abdominal") || text.contains("abdomen") -> SymptomProfile(
            conditions = listOf(
                ConditionProbability("Gastritis", 46, "Upper abdominal discomfort is frequently caused by stomach-lining inflammation."),
                ConditionProbability("Irritable bowel syndrome", 30, "Recurrent cramping with bowel changes fits a functional cause."),
                ConditionProbability("Gallbladder disease", 24, "Pain after fatty meals can point to the gallbladder."),
            ),
            specialists = listOf(
                SpecialistRecommendation("Gastroenterologist", "Abdominal pain involves the digestive tract, this specialist's domain."),
            ),
            tests = listOf(
                TestRecommendation("Liver Function Test", TestPriority.REQUIRED, "To check liver and gallbladder involvement."),
                TestRecommendation("Abdominal Ultrasound", TestPriority.RECOMMENDED, "To visualize the abdominal organs."),
                TestRecommendation("Urine Analysis", TestPriority.OPTIONAL, "To exclude urinary causes of abdominal pain."),
            ),
            scans = listOf("Abdominal Ultrasound"),
            redFlags = listOf("Severe or worsening pain", "Blood in stool or vomit", "High fever with pain"),
            reportSuggestion = "A recent abdominal ultrasound report may improve the analysis if available.",
        )
        text.contains("joint") || text.contains("knee") || text.contains("back pain") -> SymptomProfile(
            conditions = listOf(
                ConditionProbability("Muscle strain", 48, "Pain after activity most often reflects soft-tissue strain."),
                ConditionProbability("Osteoarthritis", 30, "Joint pain that worsens with use suggests wear-related changes."),
                ConditionProbability("Inflammatory arthritis", 22, "Morning stiffness and swelling can indicate joint inflammation."),
            ),
            specialists = listOf(
                SpecialistRecommendation("Orthopedic Specialist", "Joint and musculoskeletal pain is this specialist's focus."),
            ),
            tests = listOf(
                TestRecommendation("X-ray of the affected joint", TestPriority.RECOMMENDED, "To assess bone and joint-space changes."),
                TestRecommendation("ESR/CRP", TestPriority.OPTIONAL, "To look for inflammation if swelling persists."),
            ),
            scans = listOf("Joint X-ray"),
            redFlags = listOf("Inability to bear weight", "Visible deformity", "Fever with a hot swollen joint"),
            reportSuggestion = "An X-ray of the affected joint may improve the analysis if available.",
        )
        text.contains("fever") || text.contains("headache") -> SymptomProfile(
            conditions = listOf(
                ConditionProbability("Viral infection", 58, "Fever with body aches is most commonly caused by a self-limiting virus."),
                ConditionProbability("Influenza", 24, "Sudden fever with headache and fatigue is typical of the flu."),
                ConditionProbability("Typhoid", 18, "A persistent stepwise fever can indicate typhoid and should be excluded."),
            ),
            specialists = listOf(
                SpecialistRecommendation("General Physician", "Best suited for evaluating fever and flu-like symptoms."),
            ),
            tests = listOf(
                TestRecommendation("Complete Blood Count (CBC)", TestPriority.REQUIRED, "To identify signs of infection."),
                TestRecommendation("CRP", TestPriority.RECOMMENDED, "To gauge the degree of inflammation."),
                TestRecommendation("Malaria Test", TestPriority.OPTIONAL, "If fever persists or recurs in cycles."),
                TestRecommendation("Typhoid Test", TestPriority.OPTIONAL, "If fever lasts more than 5 days."),
            ),
            scans = emptyList(),
            redFlags = listOf("Persistent high fever", "Stiff neck", "Confusion or drowsiness"),
            reportSuggestion = "A recent blood report may improve the analysis if available.",
        )
        else -> SymptomProfile(
            conditions = listOf(
                ConditionProbability("Non-specific viral illness", 60, "Mild general symptoms are usually caused by a passing viral illness."),
                ConditionProbability("Fatigue-related condition", 24, "Lifestyle factors such as poor sleep can produce similar symptoms."),
                ConditionProbability("Early infection", 16, "Symptoms may still be evolving; monitoring is advised."),
            ),
            specialists = listOf(
                SpecialistRecommendation("General Physician", "Best suited for an initial evaluation of general symptoms."),
            ),
            tests = listOf(
                TestRecommendation("Complete Blood Count (CBC)", TestPriority.RECOMMENDED, "A baseline check for infection or anemia."),
            ),
            scans = emptyList(),
            redFlags = listOf("Symptoms rapidly worsening", "High fever", "Severe pain"),
            reportSuggestion = null,
        )
    }

    /** Heuristic recovery-progress summary from recent check-ins. */
    fun progress(logs: List<DailyLog>): ProgressAnalysis {
        val recent = logs.sortedByDescending { it.date }
        val latest = recent.first()
        val previous = recent.getOrNull(1)
        val compliance = recent.take(3).count { it.medicationTaken }
        val complianceLow = recent.size >= 2 && compliance <= recent.take(3).size / 2

        return when {
            previous != null && (latest.painLevel > previous.painLevel + 1 ||
                (latest.mood == Mood.POOR && previous.mood != Mood.POOR)) ->
                ProgressAnalysis(
                    "Symptoms worsening",
                    "Your recent check-ins show increasing discomfort. A follow-up with your doctor is recommended.",
                    followUpRecommended = true,
                    followUpReason = "Pain and mood have worsened compared to your previous check-in.",
                )
            complianceLow ->
                ProgressAnalysis(
                    "Medication compliance decreasing",
                    "You've missed some doses recently. Staying consistent will help your recovery.",
                    followUpRecommended = false,
                    followUpReason = "No follow-up needed yet, but missed doses are slowing your recovery.",
                )
            previous != null && (latest.painLevel < previous.painLevel ||
                latest.sleepHours >= previous.sleepHours) ->
                ProgressAnalysis(
                    "Recovery improving",
                    "Your check-ins show a positive trend. Keep following your care plan.",
                    followUpRecommended = false,
                    followUpReason = "Your pain and sleep are trending in the right direction.",
                )
            else ->
                ProgressAnalysis(
                    "Recovery stable",
                    "Your condition looks steady. Continue your care plan and daily check-ins.",
                    followUpRecommended = false,
                    followUpReason = "Your condition is stable with no warning signs.",
                )
        }
    }

    /** Offline patient-friendly translation of the doctor's outcome (Phase 5.6). */
    fun patientExplanation(prescription: Prescription): PatientExplanation = PatientExplanation(
        whatDoctorFound = "Your doctor diagnosed you with ${prescription.diagnosis.ifBlank { "a condition that needs some care" }}. " +
            "In simple terms, this is a condition your doctor knows how to manage, and the plan below is designed to help you recover. " +
            "Follow it closely and you give your body the best chance to heal.",
        medicines = prescription.medications.map {
            MedicineExplanation(
                name = it.name,
                purpose = "Take ${it.dosage}, ${it.timing.lowercase()}. This medicine supports your recovery — " +
                    "taking it consistently as prescribed is what makes it work.",
            )
        },
        whatToDo = buildList {
            add("Rest as much as you can while you recover.")
            add("Drink plenty of water through the day.")
            add("Eat light, regular meals.")
            add("Avoid smoking and alcohol.")
            if (prescription.advice.isNotBlank()) add(prescription.advice)
        },
        recovery = "Most people start feeling better within a few days of following the plan. " +
            "Gradual improvement is normal — some days may feel slower than others." +
            (prescription.followUpDate?.let { " Your doctor would like to see you again on $it." } ?: ""),
        warningSigns = listOf(
            "High fever that doesn't improve",
            "Chest pain or difficulty breathing",
            "Severe vomiting or inability to keep fluids down",
            "Symptoms getting clearly worse instead of better",
        ),
    )

    /** Generic AI wellness plan for a minor concern (patientId filled by caller). */
    fun wellnessPlan(): CarePlan = CarePlan(
        patientId = "",
        medications = listOf(
            Medication("Paracetamol", "500 mg", "If fever/pain, after food"),
        ),
        activityGoals = listOf("Light 20-minute walk", "Balanced meals"),
        recoveryGoals = listOf("Feel better within 5–7 days", "No worsening symptoms"),
        lifestyle = listOf("Rest adequately", "Avoid smoking and alcohol"),
        hydration = "Drink 8–10 glasses of water daily",
        exercise = "Gentle stretching; avoid strenuous activity",
        sleep = "Aim for 7–8 hours of sleep",
        note = "This is a self-care wellness plan for minor symptoms. See a doctor if symptoms persist or worsen.",
        source = CarePlanSource.AI_WELLNESS,
    )
}
