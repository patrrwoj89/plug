package com.polishmediahub.app.data.admin

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Result of running a sandbox test in the admin panel.
 */
data class SandboxResult(
    val success: Boolean,
    val message: String,
    val output: String? = null,
    val preview: JsonObject? = null
) {
    fun toJson(json: Json): String {
        val obj = buildJsonObject {
            put("success", success)
            put("message", message)
            if (!output.isNullOrBlank()) put("output", output)
            if (preview != null) put("preview", preview)
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    companion object {
        fun success(message: String, output: String? = null, preview: JsonObject? = null) =
            SandboxResult(success = true, message = message, output = output, preview = preview)

        fun error(message: String, output: String? = null) =
            SandboxResult(success = false, message = message, output = output)
    }
}
