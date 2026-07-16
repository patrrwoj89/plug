package com.polishmediahub.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportSanitizerTest {

    @Test
    fun `sanitize masks API key query parameters`() {
        val input = "https://example.com?api_key=supersecret12345&name=foo"
        val result = CrashReportSanitizer.sanitize(input)
        assertTrue(result.contains("api_key=****"))
        assertFalse(result.contains("supersecret12345"))
        assertTrue(result.contains("name=foo"))
    }

    @Test
    fun `sanitize masks JSON token values`() {
        val input = """{"api_key":"supersecret12345","cloudflareAuthToken":"abc123xyz"}"""
        val result = CrashReportSanitizer.sanitize(input)
        assertTrue(result.contains(""""api_key":"****""""))
        assertTrue(result.contains(""""cloudflareAuthToken":"****""""))
        assertFalse(result.contains("supersecret12345"))
        assertFalse(result.contains("abc123xyz"))
    }

    @Test
    fun `sanitize masks Authorization Bearer header`() {
        val input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val result = CrashReportSanitizer.sanitize(input)
        assertTrue(result.contains("Authorization: Bearer ****"))
        assertFalse(result.contains("eyJhbGci"))
    }

    @Test
    fun `sanitize masks form encoded secret`() {
        val input = "secret=verylongsecrethere&password=anothersecret"
        val result = CrashReportSanitizer.sanitize(input)
        assertTrue(result.contains("secret=****"))
        assertTrue(result.contains("password=****"))
        assertFalse(result.contains("verylongsecrethere"))
        assertFalse(result.contains("anothersecret"))
    }

    @Test
    fun `sanitize masks source-specific credentials`() {
        val input = "trakt_client_id=CLIENTID trakt_client_secret=CLIENTSECRET mdblist_api_key=MDBKEY"
        val result = CrashReportSanitizer.sanitize(input)
        assertTrue(result.contains("trakt_client_id=****"))
        assertTrue(result.contains("trakt_client_secret=****"))
        assertTrue(result.contains("mdblist_api_key=****"))
        assertFalse(result.contains("CLIENTID"))
        assertFalse(result.contains("CLIENTSECRET"))
        assertFalse(result.contains("MDBKEY"))
    }

    @Test
    fun `sanitize leaves short or unrelated values intact`() {
        val input = "token=abc name=Polish Media Hub"
        val result = CrashReportSanitizer.sanitize(input)
        assertTrue(result.contains("token=abc"))
        assertTrue(result.contains("name=Polish Media Hub"))
    }

    @Test
    fun `sanitize masks long token value after generic token key`() {
        val input = "Authorization: token a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
        val result = CrashReportSanitizer.sanitize(input)
        assertFalse(result.contains("a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"))
        assertTrue(result.contains("Authorization: token ****"))
    }

    @Test
    fun `sanitize preserves stack trace structure`() {
        val input = """at com.polishmediahub.app.PlayerActivity.onCreate(PlayerActivity.kt:42)
Caused by: java.lang.IllegalStateException: api_key=leakedsecret1234
at com.android.os.Looper.loop(Looper.java:288)"""
        val result = CrashReportSanitizer.sanitize(input)
        assertTrue(result.contains("at com.polishmediahub.app.PlayerActivity.onCreate"))
        assertTrue(result.contains("at com.android.os.Looper.loop"))
        assertTrue(result.contains("api_key=****"))
        assertFalse(result.contains("leakedsecret1234"))
    }

    @Test
    fun `sanitize keeps all masked values equal to constant mask`() {
        val input = "api_key=onevalue two three trakt_access_token=fourvalue five"
        val result = CrashReportSanitizer.sanitize(input)
        val matches = Regex("=\\*\\*\\*\\*").findAll(result).count()
        assertEquals(2, matches)
    }
}
