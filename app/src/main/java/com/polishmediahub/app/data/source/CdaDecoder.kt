package com.polishmediahub.app.data.source

import java.net.URLDecoder

object CdaDecoder {

    private val SPECIAL_TO_DIGIT = mapOf(
        '!' to '0',
        '@' to '1',
        '#' to '2',
        '$' to '3',
        '%' to '4'
    )

    fun decode(input: String): String {
        if (input.isBlank()) return input

        val stripped = input.replace("_b64", "")
        val digitized = stripped.map { SPECIAL_TO_DIGIT[it] ?: it }.joinToString("")
        val decoded = runCatching { URLDecoder.decode(digitized, "UTF-8") }.getOrDefault(digitized)

        return decoded.map { c ->
            val code = c.code
            if (code in 33..126) {
                (((code - 33 + 47) % 94) + 33).toChar()
            } else {
                c
            }
        }.joinToString("")
    }
}
