package com.polishmediahub.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.math.pow

class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 500
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        for (attempt in 0..maxRetries) {
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || !response.code.let { it in 500..599 }) {
                    return response
                }
                response.close()
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
}
