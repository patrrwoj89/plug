@file:OptIn(ExperimentalFoundationApi::class)

package com.polishmediahub.app.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable

@Immutable
object TvBringIntoViewSpec : BringIntoViewSpec {

    @Suppress("OVERRIDE_DEPRECATION")
    override val scrollAnimationSpec: AnimationSpec<Float> = SpringSpec(
        stiffness = 300f,
        dampingRatio = 0.8f
    )

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        if (size <= 0f || containerSize <= 0f) return 0f

        if (size >= containerSize) {
            return when {
                offset < 0f -> offset
                offset + size > containerSize -> offset + size - containerSize
                else -> 0f
            }
        }

        val targetStart = (containerSize - size) / 2f
        return offset - targetStart
    }
}

@Composable
fun TvBringIntoViewProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalBringIntoViewSpec provides TvBringIntoViewSpec,
        content = content
    )
}
