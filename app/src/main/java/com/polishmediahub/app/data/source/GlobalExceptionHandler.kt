package com.polishmediahub.app.data.source

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import com.polishmediahub.app.CrashReportActivity
import com.polishmediahub.app.data.ApiConfigRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global uncaught exception handler that writes the crash log to app files and
 * launches [CrashReportActivity] in a separate process before killing the
 * failing process.
 */
class GlobalExceptionHandler(
    private val application: Application,
    private val previousHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Log.e("GlobalExceptionHandler", "Uncaught exception on ${thread.name}", throwable)
            val crashLog = buildCrashLog(thread, throwable)
            val file = File(application.filesDir, CRASH_LOG_FILE)
            file.writeText(crashLog)

            val (workerUrl, authToken) = readCloudflareConfig()

            val intent = Intent(application, CrashReportActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(CrashReportActivity.EXTRA_CRASH_LOG_PATH, file.absolutePath)
                putExtra(CrashReportActivity.EXTRA_WORKER_URL, workerUrl)
                putExtra(CrashReportActivity.EXTRA_AUTH_TOKEN, authToken)
            }
            application.startActivity(intent)

            // Allow the activity manager to launch the crash-report process before we die.
            try {
                Thread.sleep(HANDOFF_DELAY_MS)
            } catch (e: InterruptedException) {
                Log.w("GlobalExceptionHandler", "Hand-off sleep interrupted: ${e.message}")
            }

            previousHandler?.uncaughtException(thread, throwable)
        } catch (e: Exception) {
            Log.e("GlobalExceptionHandler", "Failed to launch crash reporter", e)
        } finally {
            Process.killProcess(Process.myPid())
            System.exit(10)
        }
    }

    private fun buildCrashLog(thread: Thread, throwable: Throwable): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return buildString {
            appendLine("Crash report")
            appendLine("Time: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine("Process: ${Process.myPid()}")
            appendLine()
            append(Log.getStackTraceString(throwable))
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ApiConfigEntryPoint {
        fun apiConfigRepository(): ApiConfigRepository
    }

    private fun readCloudflareConfig(): Pair<String, String> {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                application,
                ApiConfigEntryPoint::class.java
            )
            val repository = entryPoint.apiConfigRepository()
            val workerUrl = runBlocking { repository.cloudflareWorkerUrl.first() }
            val authToken = runBlocking { repository.cloudflareAuthToken.first() }
            workerUrl to authToken
        } catch (_: Exception) {
            "" to ""
        }
    }

    companion object {
        const val CRASH_LOG_FILE = "last_crash.txt"
        const val HANDOFF_DELAY_MS = 400L

        /**
         * Returns true if this process is the main application process and should
         * install the crash handler. The crash-report process intentionally does
         * not install the handler to avoid recursion if the reporter itself fails.
         */
        fun shouldInstallHandler(context: Context): Boolean {
            return !getProcessName(context).contains(":crashreport")
        }

        @Suppress("deprecation")
        private fun getProcessName(context: Context): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return Application.getProcessName() ?: ""
            }
            val pid = Process.myPid()
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            return manager?.runningAppProcesses
                ?.find { it.pid == pid }
                ?.processName
                ?: ""
        }
    }
}
