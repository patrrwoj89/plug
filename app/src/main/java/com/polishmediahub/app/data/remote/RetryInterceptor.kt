package com.polishmediahub.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.pow

class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 500,
    private val maxRetryAfterMs: Long = 60_000
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        for (attempt in 0..maxRetries) {
            try {
                val response = chain.proceed(request)

                if (response.isSuccessful) {
                    return response
                }

                when (response.code) {
                    429 -> {
                        val retryAfter = response.header("Retry-After")
                        response.close()
                        val delayMs = parseRetryAfterMs(retryAfter, attempt)
                        if (attempt < maxRetries && delayMs > 0) {
                            Thread.sleep(delayMs.coerceAtMost(maxRetryAfterMs))
                        }
                    }
                    in 500..599 -> {
                        response.close()
                    }
                    else -> {
                        // Do not retry client errors (4xx) or 401; let the Authenticator / caller handle them.
                        return response
                    }
                }
            } catch (e: IOException) {
                lastException = e
            }

            if (attempt < maxRetries) {
                val delay = baseDelayMs * 2.0.pow(attempt.toDouble()).toLong()
                Thread.sleep(delay)
            }
        }

        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }

    private fun parseRetryAfterMs(retryAfter: String?, attempt: Int): Long {
        if (retryAfter.isNullOrBlank()) {
            return baseDelayMs * 2.0.pow(attempt.toDouble()).toLong()
        }

        // Retry-After can be a non-negative decimal integer of seconds.
        retryAfter.trim().toLongOrNull()?.let {
            return it * 1000L
        }

        // Retry-After can also be an HTTP-date (RFC 7231).
        try {
            val date = HTTP_DATE_FORMAT.parse(retryAfter.trim())
            if (date != null) {
                return (date.time - System.currentTimeMillis()).coerceAtLeast(0L)
            }
        } catch (_: ParseException) {
        }

        return baseDelayMs * 2.0.pow(attempt.toDouble()).toLong()
    }

    companion object {
        private val HTTP_DATE_FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            .apply { isLenient = false }
    }
}
