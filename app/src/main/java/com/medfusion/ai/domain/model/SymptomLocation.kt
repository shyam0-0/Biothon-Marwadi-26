package com.medfusion.ai.domain.model

/**
 * Smart symptom localization (Phase 5.6): the patient marks WHERE symptoms occur
 * on a simple multi-view body map, then answers what it feels like, how severe it
 * is, when it began and how it's progressing. This travels with the case to the
 * doctor, into the AI prompt, and into the passport's AI history.
 */

/** The viewpoint tabs of the body map. */
enum class BodyView(val label: String) {
    FRONT("Front"),
    BACK("Back"),
    LEFT("Left Side"),
    RIGHT("Right Side"),
}

/** Predefined selectable regions. [views] lists where the region is tappable. */
enum class BodyRegion(val wireValue: String, val label: String, val views: Set<BodyView>) {
    HEAD("head", "Head", setOf(BodyView.FRONT, BodyView.BACK, BodyView.LEFT, BodyView.RIGHT)),
    FACE("face", "Face", setOf(BodyView.FRONT)),
    NECK("neck", "Neck", setOf(BodyView.FRONT, BodyView.BACK, BodyView.LEFT, BodyView.RIGHT)),
    LEFT_SHOULDER("left_shoulder", "Left Shoulder", setOf(BodyView.FRONT, BodyView.BACK, BodyView.LEFT)),
    RIGHT_SHOULDER("right_shoulder", "Right Shoulder", setOf(BodyView.FRONT, BodyView.BACK, BodyView.RIGHT)),
    CHEST("chest", "Chest", setOf(BodyView.FRONT)),
    UPPER_ABDOMEN("upper_abdomen", "Upper Abdomen", setOf(BodyView.FRONT)),
    LOWER_ABDOMEN("lower_abdomen", "Lower Abdomen", setOf(BodyView.FRONT)),
    BACK_UPPER("back", "Back", setOf(BodyView.BACK)),
    LOWER_BACK("lower_back", "Lower Back", setOf(BodyView.BACK)),
    LEFT_ARM("left_arm", "Left Arm", setOf(BodyView.FRONT, BodyView.BACK, BodyView.LEFT)),
    RIGHT_ARM("right_arm", "Right Arm", setOf(BodyView.FRONT, BodyView.BACK, BodyView.RIGHT)),
    LEFT_HAND("left_hand", "Left Hand", setOf(BodyView.FRONT, BodyView.BACK, BodyView.LEFT)),
    RIGHT_HAND("right_hand", "Right Hand", setOf(BodyView.FRONT, BodyView.BACK, BodyView.RIGHT)),
    PELVIS("pelvis", "Pelvis", setOf(BodyView.FRONT)),
    LEFT_HIP("left_hip", "Left Hip", setOf(BodyView.FRONT, BodyView.BACK, BodyView.LEFT)),
    RIGHT_HIP("right_hip", "Right Hip", setOf(BodyView.FRONT, BodyView.BACK, BodyView.RIGHT)),
    LEFT_LEG("left_leg", "Left Leg", setOf(BodyView.FRONT, BodyView.BACK, BodyView.LEFT)),
    RIGHT_LEG("right_leg", "Right Leg", setOf(BodyView.FRONT, BodyView.BACK, BodyView.RIGHT)),
    LEFT_KNEE("left_knee", "Left Knee", setOf(BodyView.FRONT, BodyView.LEFT)),
    RIGHT_KNEE("right_knee", "Right Knee", setOf(BodyView.FRONT, BodyView.RIGHT)),
    LEFT_FOOT("left_foot", "Left Foot", setOf(BodyView.FRONT, BodyView.BACK, BodyView.LEFT)),
    RIGHT_FOOT("right_foot", "Right Foot", setOf(BodyView.FRONT, BodyView.BACK, BodyView.RIGHT));

    companion object {
        fun fromWire(value: String?): BodyRegion? =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) }
    }
}

/** One localized symptom: a region plus the follow-up answers. */
data class SymptomLocation(
    val region: BodyRegion,
    val descriptor: String,   // e.g. Pain, Burning, Numbness…
    val severity: Int,        // 0–10
    val duration: String,     // Today | Yesterday | Several days | Weeks | Months
    val progression: String,  // Better | Same | Worse
) {
    /** One-line summary used by the prompt, the doctor pre-read and the passport. */
    fun summary(): String =
        "${region.label}: $descriptor, severity $severity/10, started ${duration.lowercase()}, " +
            "getting ${progression.lowercase()}"
}

/** The fixed answer options shown after a region is selected. */
object SymptomLocalizationOptions {
    val descriptors = listOf(
        "Pain", "Burning", "Numbness", "Tingling", "Weakness",
        "Swelling", "Stiffness", "Rash", "Injury", "Other",
    )
    val durations = listOf("Today", "Yesterday", "Several days", "Weeks", "Months")
    val progressions = listOf("Better", "Same", "Worse")
}
