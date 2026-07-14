package com.tvhub.skeleton.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.tvhub.skeleton.ui.theme.AppColor
import com.tvhub.skeleton.ui.theme.Focus
import com.tvhub.skeleton.ui.theme.Motion
import com.tvhub.skeleton.ui.theme.Radius

/**
 * Modifier that adds D-Pad/TV-friendly focus behavior: scale, outline and subtle glow.
 */
fun Modifier.tvFocusable(
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    scale: Float = Focus.scale,
    outlineColor: Color = AppColor.FocusOutline,
    glowColor: Color = AppColor.FocusGlow,
    shape: Shape = RoundedCornerShape(Radius.md),
    onClick: (() -> Unit)? = null
): Modifier = this.composed {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) scale else 1f,
        animationSpec = tween(Motion.focus),
        label = "focus-scale"
    )

    val focusedModifier = if (isFocused) {
        Modifier
            .border(Focus.outlineWidth, outlineColor, shape)
            .background(
                brush = Brush.radialGradient(
                    0.0f to glowColor,
                    1.0f to Color.Transparent,
                    radius = 200f
                ),
                shape = shape
            )
    } else Modifier

    this
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                )
            } else {
                Modifier.focusable(interactionSource = interactionSource)
            }
        )
        .onFocusChanged { }
        .scale(animatedScale)
        .then(focusedModifier)
}

@Composable
fun FocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.md),
    scale: Float = Focus.scale,
    backgroundColor: Color = AppColor.SurfaceVariant,
    focusedBackgroundColor: Color = AppColor.SurfaceHover,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) scale else 1f,
        animationSpec = tween(Motion.focus),
        label = "surface-scale"
    )

    Box(
        modifier = modifier
            .scale(animatedScale)
            .tvFocusable(
                interactionSource = interactionSource,
                scale = 1f, // scale already applied above
                shape = shape,
                onClick = onClick
            )
            .background(
                if (isFocused) focusedBackgroundColor else backgroundColor,
                shape
            ),
        contentAlignment = contentAlignment,
        content = content
    )
}
