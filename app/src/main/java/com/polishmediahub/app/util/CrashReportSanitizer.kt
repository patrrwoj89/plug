package com.polishmediahub.app.util

/**
 * Redacts potentially sensitive API keys, OAuth tokens, passwords and premium
 * credentials from a crash-report stacktrace before it leaves the device.
 *
 * The sanitizer operates on plain strings and has no Android dependencies, so
 * it can be unit-tested in isolation (Zasada 3 / Zasada 1).
 */
object CrashReportSanitizer {

    private const val MASK = "****"

    // Token-like keys that are commonly used in headers, JSON, form data and URLs.
    private val SENSITIVE_KEYS = listOf(
        "api[-_]?key",
        "auth[-_]?token",
        "access[-_]?token",
        "refresh[-_]?token",
        "bearer",
        "secret",
        "password",
        "token",
        "cloudflare[-_]?auth[-_]?token",
        "hub[-_]?token",
        "debrid[-_]?api[-_]?key",
        "debrid[-_]?access[-_]?token",
        "debrid[-_]?refresh[-_]?token",
        "trakt[-_]?client[-_]?id",
        "trakt[-_]?client[-_]?secret",
        "trakt[-_]?access[-_]?token",
        "trakt[-_]?refresh[-_]?token",
        "mdblist[-_]?api[-_]?key",
        "tmdb[-_]?api[-_]?key",
        "ani[-_]?list[-_]?token",
        "kodi[-_]?(?:url|token|username|password)",
        "jellyfin[-_]?(?:url|token)",
        "plex[-_]?(?:url|token)",
        "emby[-_]?(?:url|token)",
        "subsonic[-_]?(?:url|user|password)"
    ).joinToString("|")

    /**
     * Matches header/JSON/form entries such as:
     *   api_key=abcdef12345
     *   "api_key": "abcdef12345"
     *   X-Hub-Token: abcdef12345
     */
    private val KEY_VALUE_RE =
        "(?i)((?:[\"']?)(?:$SENSITIVE_KEYS)(?:[\"']?)\\s*[:=]\\s*[\"']?)([^\"'\\s,&;<>]{4,})([\"']?)".toRegex()

    /**
     * Matches URL query parameters such as:
     *   ?api_key=abcdef&secret=xyz
     */
    private val QUERY_PARAM_RE =
        "(?i)([?&](?:$SENSITIVE_KEYS)=)([^&\\s\"']{4,})".toRegex()

    private val AUTHORIZATION_RE =
        "(?i)(authorization\\s*:\\s*(?:bearer|token|basic)\\s+)([a-zA-Z0-9_.=:-]{8,})".toRegex()

    /**
     * Returns a copy of [text] with detected secrets replaced by [MASK].
     */
    fun sanitize(text: String): String {
        var result = text
        result = AUTHORIZATION_RE.replace(result) { match ->
            match.groupValues[1] + MASK
        }
        result = KEY_VALUE_RE.replace(result) { match ->
            match.groupValues[1] + MASK + match.groupValues[3]
        }
        result = QUERY_PARAM_RE.replace(result) { match ->
            match.groupValues[1] + MASK
        }
        return result
    }
}
