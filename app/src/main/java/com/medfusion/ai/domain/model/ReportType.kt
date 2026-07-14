package com.medfusion.ai.domain.model

/** The kinds of medical report a patient can upload for a case (Phase 3). */
enum class ReportType(val storageKey: String, val label: String) {
    XRAY("xray", "X-ray / Scan"),
    LAB_REPORT("lab_report", "Lab Report"),
}
