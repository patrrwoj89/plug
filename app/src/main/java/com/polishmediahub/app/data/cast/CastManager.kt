package com.polishmediahub.app.data.cast

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun isCastAvailable(): Boolean = false
    fun isCasting(): Boolean = false
    fun startCasting(routeId: String): Boolean = false
    fun stopCasting() {}
    fun castMedia(title: String, url: String, positionMs: Long = 0L): Boolean = false
}
