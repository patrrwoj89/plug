package com.polishmediahub.app.data.plugin

import com.whl.quickjs.wrapper.QuickJSContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickJsEngine @Inject constructor() {

    private var context: QuickJSContext? = null

    fun init() {
        context = QuickJSContext.create()
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
}
