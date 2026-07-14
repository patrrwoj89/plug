package com.tvhub.skeleton.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.tvhub.skeleton.ui.theme.AppColor
import com.tvhub.skeleton.ui.theme.Radius
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.md),
    baseColor: Color = AppColor.SurfaceVariant,
    highlightColor: Color = AppColor.SurfaceHover
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate = transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-translate"
    ).value

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(0f, 0f),
        end = Offset(translate * 1000f, 0f)
    )

    Box(
        modifier = modifier
            .background(brush, shape)
            .fillMaxSize()
    )
}
