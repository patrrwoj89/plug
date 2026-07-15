package com.polishmediahub.app.data.source

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryCookieJar @Inject constructor() : CookieJar {

    private val allCookies = Collections.synchronizedList(mutableListOf<Cookie>())

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(allCookies) {
            for (cookie in cookies) {
                allCookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
                if (cookie.expiresAt > System.currentTimeMillis()) {
                    allCookies.add(cookie)
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return synchronized(allCookies) {
            allCookies.removeAll { it.expiresAt <= now }
            allCookies.filter { it.matches(url) }
        }
    }

    fun storeCookies(url: HttpUrl, cookies: List<Cookie>) = saveFromResponse(url, cookies)

    fun cookieHeader(url: HttpUrl): String? {
        val cookies = loadForRequest(url)
        if (cookies.isEmpty()) return null
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }
}
