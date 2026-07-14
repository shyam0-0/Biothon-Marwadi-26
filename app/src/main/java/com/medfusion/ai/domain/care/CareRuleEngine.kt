package com.medfusion.ai.domain.care

import com.medfusion.ai.domain.model.ActivityLevel
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CareSuggestion
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.Mood
import javax.inject.Inject

/**
 * Simple, transparent rule-based engine (deliberately not ML) that turns a
 * patient's recent daily logs into adaptive care suggestions.
 *
 * Safety rule: any suggestion touching medication is flagged
 * [CareSuggestion.requiresDoctorApproval] so the app routes it for approval
 * instead of applying it automatically.
 */
class CareRuleEngine @Inject constructor() {

    /**
     * @param logs recent logs; order-independent (sorted here, newest first).
     */
    fun evaluate(plan: CarePlan?, logs: List<DailyLog>): List<CareSuggestion> {
        if (logs.isEmpty()) return emptyList()
        val recent = logs.sortedByDescending { it.date }
        val lastTwo = recent.take(2)
        val hasTwoDays = lastTwo.size >= 2

        val suggestions = mutableListOf<CareSuggestion>()

        // Lifestyle: sustained poor sleep or low activity → adjust the routine.
        val poorSleepStreak = hasTwoDays && lastTwo.all { it.sleepHours < 5.0 }
        val lowActivityStreak = hasTwoDays && lastTwo.all { it.activityLevel == ActivityLevel.LOW }
        if (poorSleepStreak || lowActivityStreak) {
            suggestions += CareSuggestion(
                message = "You've had two quieter days. Try moving today's walk to the evening " +
                    "and winding down 30 minutes earlier tonight.",
                requiresDoctorApproval = false,
            )
        }

        // Medication-related: sustained low mood while on medication → needs a
        // doctor to review timing/dosage. Never applied automatically.
        val lowMoodStreak = hasTwoDays && lastTwo.all { it.mood == Mood.POOR }
        val onMedication = plan?.medications?.isNotEmpty() == true
        if (lowMoodStreak && onMedication) {
            suggestions += CareSuggestion(
                message = "Your mood has been low for a couple of days. Your doctor may want to " +
                    "review your medication timing.",
                requiresDoctorApproval = true,
            )
        }

        return suggestions
    }
}
