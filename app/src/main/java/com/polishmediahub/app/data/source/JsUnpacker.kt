package com.polishmediahub.app.data.source

object JsUnpacker {

    // Matches the typical eval(function(p,a,c,k,e,...){...}('packed',62,...,'dict'.split('|'),0,{})) call.
    private val PACKER_REGEX = Regex(
        """eval\s*\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*[^)]+\s*\)\s*\{[^}]*while\s*\([^)]*\)[^}]*return\s+p[^}]*\}\s*\(\s*(['"])([^'"]*)\1\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(['"])([^'"]*)\5\.split\(['"]\|['"]\)\s*,\s*\d+\s*,\s*\{\}\s*\)\s*\)""",
        RegexOption.DOT_MATCHES_ALL
    )

    fun unpack(packedJs: String): String {
        val match = PACKER_REGEX.find(packedJs) ?: return packedJs

        val packed = match.groupValues[2]
        val radix = match.groupValues[3].toIntOrNull() ?: 10
        val count = match.groupValues[4].toIntOrNull() ?: 0
        val dictStr = match.groupValues[6]
        val keywords = dictStr.split('|')

        return unpack(packed, radix, count, keywords)
    }

    fun unpack(packed: String, radix: Int, count: Int, keywords: List<String>): String {
        if (radix < 2 || radix > 62) return packed
        var result = packed
        for (i in (count - 1) downTo 0) {
            val keyword = keywords.getOrNull(i) ?: continue
            if (keyword.isEmpty()) continue
            val token = intToToken(i, radix)
            val regex = "\\b$token\\b".toRegex()
            result = result.replace(regex, keyword)
        }
        return result
    }

    private fun intToToken(value: Int, radix: Int): String {
        if (value < radix) return charFor(value).toString()
        val sb = StringBuilder()
        var v = value
        while (v > 0) {
            sb.append(charFor(v % radix))
            v /= radix
        }
        return sb.reverse().toString()
    }

    private fun charFor(value: Int): Char = when {
        value < 10 -> '0' + value
        value < 36 -> 'a' + (value - 10)
        value < 62 -> 'A' + (value - 36)
        else -> '?'
    }
}
