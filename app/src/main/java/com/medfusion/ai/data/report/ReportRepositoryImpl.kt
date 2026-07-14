package com.medfusion.ai.data.report

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medfusion.ai.BuildConfig
import com.medfusion.ai.R
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.core.util.toAppError
import com.medfusion.ai.data.remote.MedFusionApi
import com.medfusion.ai.data.remote.dto.GeneratePdfRequest
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.repository.ReportDownloadResult
import com.medfusion.ai.domain.repository.ReportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MedFusionApi,
    private val pdfGenerator: PdfReportGenerator,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ReportRepository {

    override suspend fun downloadReport(case: Case): Resource<ReportDownloadResult> =
        withContext(io) {
            resourceOf {
                val url = fetchPdfUrl(case.caseId)
                if (url != null) {
                    enqueueSystemDownload(url, case.caseId)
                    ReportDownloadResult.Enqueued
                } else {
                    val fileName = pdfGenerator.generateAndSave(case)
                    notifyLocalSave(fileName)
                    ReportDownloadResult.SavedLocally(fileName)
                }
            }
        }

    /**
     * Returns the server PDF URL, or null to signal the on-device fallback should
     * be used (only when the mock fallback is enabled and the error is recoverable).
     */
    private suspend fun fetchPdfUrl(caseId: String): String? = try {
        api.generatePdf(GeneratePdfRequest(caseId)).pdfUrl
    } catch (t: Throwable) {
        val error = t.toAppError()
        val recoverable = error is AppError.Network || error is AppError.Timeout || error is AppError.Server
        if (BuildConfig.USE_MOCK_AI_FALLBACK && recoverable) null else throw t
    }

    private fun enqueueSystemDownload(url: String, caseId: String) {
        val fileName = "MedFusion-Report-${caseId.take(8)}.pdf"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("MedFusion Health Report")
            .setDescription("Downloading your report…")
            .setMimeType("application/pdf")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }

    /** Posts a completion notification for the on-device (fallback) PDF save. */
    @SuppressLint("MissingPermission")
    private fun notifyLocalSave(fileName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Report downloads",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Report saved")
            .setContentText("$fileName is in your Downloads folder.")
            .setAutoCancel(true)
            .build()
        // POST_NOTIFICATIONS is requested by the UI before invoking a download.
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val CHANNEL_ID = "report_downloads"
        const val NOTIFICATION_ID = 4201
    }
}
