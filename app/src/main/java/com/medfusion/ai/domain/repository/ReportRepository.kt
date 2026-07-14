package com.medfusion.ai.domain.repository

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.Case

/**
 * Produces and downloads a patient's PDF report (Phase 6). Backed by the
 * /generate-pdf endpoint + Android DownloadManager, with an on-device PDF
 * generator fallback for demo/offline builds.
 */
interface ReportRepository {
    suspend fun downloadReport(case: Case): Resource<ReportDownloadResult>
}

sealed interface ReportDownloadResult {
    /** DownloadManager is fetching the server PDF; it posts its own completion notification. */
    data object Enqueued : ReportDownloadResult

    /** A PDF was rendered on-device and saved to the Downloads folder. */
    data class SavedLocally(val fileName: String) : ReportDownloadResult
}
