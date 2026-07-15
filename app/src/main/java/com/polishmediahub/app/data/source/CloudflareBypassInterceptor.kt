package com.polishmediahub.app.data.source

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class CloudflareBypassInterceptor @Inject constructor(
    private val headlessWebSolver: HeadlessWebSolver
) : Interceptor {

    companion object {
        const val BROWSER_USER_AGENT = HeadlessWebSolver.BROWSER_USER_AGENT
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = normalizeRequest(original)

        val response = chain.proceed(request)

        if (shouldAttemptBypass(request, response)) {
            response.close()
            val cookies = runBlocking { headlessWebSolver.solveAndGetCookies(request.url.toString()) }
            if (cookies.isNotBlank()) {
                val retry = normalizeRequest(
                    original.newBuilder()
                        .header("X-Cf-Solved", "1")
                        .build()
                )
                return chain.proceed(retry)
            }
        }

        return response
    }

    private fun normalizeRequest(request: okhttp3.Request): okhttp3.Request {
        val referer = request.header("X-Set-Referer") ?: ("${request.url.scheme}://${request.url.host}")
        return request.newBuilder()
            .header("User-Agent", BROWSER_USER_AGENT)
            .removeHeader("X-Set-Referer")
            .header("Referer", referer)
            .build()
    }

    private fun shouldAttemptBypass(request: okhttp3.Request, response: Response): Boolean {
        if (request.header("X-Cf-Solved") != null) return false
        if (response.code !in 403..504) return false
        val server = response.header("Server") ?: ""
        return server.contains("cloudflare", ignoreCase = true) ||
            response.header("CF-RAY") != null ||
            response.header("cf-ray") != null
    }
}
