package com.polishmediahub.app.data.plugin

import com.whl.quickjs.wrapper.JSCallFunction
import com.whl.quickjs.wrapper.JSObject
import com.whl.quickjs.wrapper.QuickJSContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class QuickJsEngine @Inject constructor(
    private val client: OkHttpClient
) {

    private var context: QuickJSContext? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun init() {
        context = QuickJSContext.create().also { ctx ->
            val global = ctx.globalObject
            global.setProperty("httpFetch", JSCallFunction { args -> httpFetch(args) })
            global.setProperty("httpFetchText", JSCallFunction { args -> httpFetch(args) })
        }
    }

    fun evaluate(script: String): Any? {
        return try {
            context?.evaluate(script, "plugin.js")
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        context?.destroy()
        context = null
    }

    private fun httpFetch(args: Array<out Any?>): String {
        val url = args.getOrNull(0) as? String ?: return errorJson("Missing URL")
        val headersMap = extractHeaders(args.getOrNull(1))

        val request = Request.Builder()
            .url(url)
            .apply {
                headersMap.forEach { (key, value) ->
                    if (key.equals("User-Agent", ignoreCase = true)) {
                        header("User-Agent", value)
                    } else {
                        header(key, value)
                    }
                }
                if (!headersMap.containsKey("User-Agent") && !headersMap.containsKey("user-agent")) {
                    header("User-Agent", "Mozilla/5.0 (Android TV; PolishMediaHub)")
                }
            }
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val headers = mutableMapOf<String, String>()
                response.headers.forEach { (name, value) ->
                    headers[name] = value
                }
                val body = response.body?.string().orEmpty()
                json.encodeToString(
                    HttpResponse.serializer(),
                    HttpResponse(
                        code = response.code,
                        body = body,
                        headers = headers
                    )
                )
            }
        } catch (e: Exception) {
            errorJson(e.message ?: "Network error")
        }
    }

    private fun errorJson(message: String): String {
        return json.encodeToString(
            HttpResponse.serializer(),
            HttpResponse(code = 0, body = "", headers = emptyMap(), error = message)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractHeaders(input: Any?): Map<String, String> {
        return when (input) {
            is JSObject -> {
                input.toMap()
                    .mapNotNull { (key, value) ->
                        val k = key as? String ?: return@mapNotNull null
                        val v = value as? String ?: value?.toString() ?: return@mapNotNull null
                        k to v
                    }
                    .toMap()
            }
            is Map<*, *> -> {
                input.mapNotNull { (key, value) ->
                    val k = key as? String ?: return@mapNotNull null
                    val v = value as? String ?: value?.toString() ?: return@mapNotNull null
                    k to v
                }.toMap()
            }
            else -> emptyMap()
        }
    }

    @Serializable
    private data class HttpResponse(
        val code: Int,
        val body: String,
        val headers: Map<String, String>,
        val error: String? = null
    )
}
