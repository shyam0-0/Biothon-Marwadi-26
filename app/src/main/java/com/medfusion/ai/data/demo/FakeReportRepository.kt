package com.medfusion.ai.data.demo

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.data.report.PdfReportGenerator
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.repository.ReportDownloadResult
import com.medfusion.ai.domain.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Demo Mode report download: generates a real on-device PDF into Downloads. */
@Singleton
class FakeReportRepository @Inject constructor(
    private val pdfGenerator: PdfReportGenerator,
) : ReportRepository {
    override suspend fun downloadReport(case: Case): Resource<ReportDownloadResult> =
        withContext(Dispatchers.IO) {
            resourceOf {
                val fileName = pdfGenerator.generateAndSave(case)
                ReportDownloadResult.SavedLocally(fileName)
            }
        }
}
