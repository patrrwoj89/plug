package com.polishmediahub.app.data.source

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import dagger.hilt.android.qualifiers.ApplicationContext
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeadlessWebSolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cookieJar: MemoryCookieJar
) {

    companion object {
        const val BROWSER_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Chromecast) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        private const val TIMEOUT_MS = 15_000L
        private const val POLL_MS = 500L
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun solveAndGetCookies(url: String): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            val webView = WebView(context)
            val cookieManager = CookieManager.getInstance()

            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = BROWSER_USER_AGENT
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webView.webChromeClient = WebChromeClient()

            var finished = false
            val start = System.currentTimeMillis()

            lateinit var timeoutRunnable: Runnable

            fun finish(cookies: String) {
                if (finished) return
                finished = true
                handler.removeCallbacks(timeoutRunnable)
                injectCookiesIntoJar(url, cookies)
                webView.stopLoading()
                webView.destroy()
                if (cont.isActive) cont.resumeWith(Result.success(cookies))
            }

            timeoutRunnable = object : Runnable {
                override fun run() {
                    if (finished) return
                    val elapsed = System.currentTimeMillis() - start
                    if (elapsed >= TIMEOUT_MS) {
                        finish("")
                        return
                    }
                    val cookies = cookieManager.getCookie(url) ?: ""
                    if (cookies.contains("cf_clearance", ignoreCase = true) || elapsed > 3_000) {
                        handler.postDelayed({ finish(cookies) }, 500)
                    } else {
                        handler.postDelayed(this, POLL_MS)
                    }
                }
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, pageUrl: String?) {
                    handler.postDelayed(timeoutRunnable, 250)
                }
            }

            cont.invokeOnCancellation {
                finish("")
            }

            webView.loadUrl(url)
            handler.postDelayed(timeoutRunnable, TIMEOUT_MS)
        }
    }

    private fun injectCookiesIntoJar(url: String, cookieString: String) {
        val httpUrl = url.toHttpUrlOrNull() ?: return
        val host = httpUrl.host
        val cookies = parseCookieString(httpUrl, cookieString, host)
        if (cookies.isNotEmpty()) {
            cookieJar.storeCookies(httpUrl, cookies)
        }
    }

    private fun parseCookieString(httpUrl: HttpUrl, cookieString: String, host: String): List<Cookie> {
        if (cookieString.isBlank()) return emptyList()
        val now = System.currentTimeMillis()
        return cookieString.split(";").mapNotNull { pair ->
            val trimmed = pair.trim()
            val eq = trimmed.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            val name = trimmed.substring(0, eq).trim()
            val value = trimmed.substring(eq + 1).trim()
            if (name.isEmpty()) return@mapNotNull null
            runCatching {
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .hostOnlyDomain(host)
                    .path("/")
                    .expiresAt(now + 365L * 24 * 60 * 60 * 1000)
                    .build()
            }.getOrNull()
        }
    }
}
