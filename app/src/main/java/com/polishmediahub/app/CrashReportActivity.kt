package com.polishmediahub.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.polishmediahub.app.data.source.GlobalExceptionHandler
import com.polishmediahub.app.ui.components.TvButton
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.theme.TVHubTheme
import com.polishmediahub.app.util.CrashReportSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class CrashReportActivity : ComponentActivity() {

    private val reportStatus = mutableStateOf<String?>(null)
    private val reportStatusColor = mutableStateOf<Color>(AppColor.Success)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val logPath = intent?.getStringExtra(EXTRA_CRASH_LOG_PATH)
            ?: File(filesDir, GlobalExceptionHandler.CRASH_LOG_FILE).absolutePath
        val stackTrace = readCrashLog(logPath)

        val workerUrl = intent?.getStringExtra(EXTRA_WORKER_URL).orEmpty()
        val authToken = intent?.getStringExtra(EXTRA_AUTH_TOKEN).orEmpty()
        val isSendConfigured = workerUrl.isNotBlank() && authToken.isNotBlank()

        setContent {
            TVHubTheme(darkTheme = true) {
                CrashReportScreen(
                    stackTrace = stackTrace,
                    isSendConfigured = isSendConfigured,
                    sendStatus = reportStatus.value,
                    sendStatusColor = reportStatusColor.value,
                    onRestart = { restartApp(clearCache = false) },
                    onClearCacheAndRestart = { restartApp(clearCache = true) },
                    onSendReport = { sendCrashReport(workerUrl, authToken, stackTrace) }
                )
            }
        }
    }

    private fun readCrashLog(path: String): String {
        return try {
            File(path).inputStream().bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            getString(R.string.crash_report_no_details)
        }
    }

    private fun sendCrashReport(workerUrl: String, authToken: String, stackTrace: String) {
        if (workerUrl.isBlank() || authToken.isBlank()) {
            reportStatus.value = getString(R.string.crash_report_not_configured)
            reportStatusColor.value = AppColor.Error
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val success = sendToCloud(workerUrl, authToken, stackTrace)
            withContext(Dispatchers.Main) {
                if (success) {
                    reportStatus.value = getString(R.string.crash_report_sent)
                    reportStatusColor.value = AppColor.Success
                } else {
                    reportStatus.value = getString(R.string.crash_report_send_error)
                    reportStatusColor.value = AppColor.Error
                }
            }
        }
    }

    private fun sendToCloud(workerUrl: String, authToken: String, stackTrace: String): Boolean {
        val sanitized = CrashReportSanitizer.sanitize(stackTrace)
        val endpoint = workerUrl.removeSuffix("/") + "/report-error"

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val body = sanitized.toRequestBody("text/plain".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(endpoint)
            .header("X-Hub-Token", authToken)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        } finally {
            client.dispatcher.cancelAll()
            client.connectionPool.evictAll()
        }
    }

    private fun restartApp(clearCache: Boolean) {
        if (clearCache) {
            clearTempFiles()
        }
        deleteCrashLog()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun clearTempFiles() {
        try {
            applicationContext.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            File(applicationContext.codeCacheDir, "plugins_dex").deleteRecursively()
        } catch (_: Exception) {
            // Best-effort cleanup; do not block restart if it fails.
        }
    }

    private fun deleteCrashLog() {
        try {
            File(filesDir, GlobalExceptionHandler.CRASH_LOG_FILE).delete()
        } catch (_: Exception) {
            // ignore
        }
    }

    companion object {
        const val EXTRA_CRASH_LOG_PATH = "crash_log_path"
        const val EXTRA_WORKER_URL = "worker_url"
        const val EXTRA_AUTH_TOKEN = "auth_token"
    }
}

@Composable
private fun CrashReportScreen(
    stackTrace: String,
    isSendConfigured: Boolean,
    sendStatus: String?,
    sendStatusColor: Color,
    onRestart: () -> Unit,
    onClearCacheAndRestart: () -> Unit,
    onSendReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = AppColor.Error
        )

        Text(
            text = stringResource(id = R.string.crash_report_title),
            style = AppTypography.headline,
            color = AppColor.Error,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(id = R.string.crash_report_hint),
            style = AppTypography.body,
            color = AppColor.OnSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            item {
                Text(
                    text = stackTrace,
                    style = AppTypography.caption.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    color = AppColor.OnSurfaceVariant
                )
            }
        }

        if (!sendStatus.isNullOrBlank()) {
            Text(
                text = sendStatus,
                style = AppTypography.body,
                color = sendStatusColor,
                textAlign = TextAlign.Center
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally)
        ) {
            TvButton(onClick = onRestart) {
                Text(
                    text = stringResource(id = R.string.crash_report_restart),
                    style = AppTypography.button,
                    color = AppColor.Black
                )
            }

            TvButton(onClick = onClearCacheAndRestart) {
                Text(
                    text = stringResource(id = R.string.crash_report_clear_cache),
                    style = AppTypography.button,
                    color = AppColor.Black
                )
            }

            TvButton(
                onClick = onSendReport,
                enabled = isSendConfigured
            ) {
                Text(
                    text = stringResource(id = R.string.crash_report_send),
                    style = AppTypography.button,
                    color = AppColor.Black
                )
            }
        }
    }
}
