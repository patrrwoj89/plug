package com.polishmediahub.app.ui.components

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Focus
import com.polishmediahub.app.ui.theme.Motion
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing

/**
 * Modifier that adds D-Pad/TV-friendly focus behavior: scale, outline and subtle glow.
 *
 * When [focusTarget] is `false` only the visual effects are applied. The caller must
 * connect the same [interactionSource] to an inner focusable component (e.g.
 * [OutlinedTextField]) so the visual state tracks the real focus.
 */
fun Modifier.tvFocusable(
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    scale: Float = Focus.scale,
    outlineColor: Color = AppColor.FocusOutline,
    glowColor: Color = AppColor.FocusGlow,
    shape: Shape = RoundedCornerShape(Radius.md),
    focusTarget: Boolean = true,
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

    val focusModifier = if (focusTarget) {
        if (onClick != null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
        } else {
            Modifier.focusable(interactionSource = interactionSource)
        }
    } else Modifier

    this
        .then(focusModifier)
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
                scale = 1f,
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

@Composable
fun TvButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    FocusableSurface(
        onClick = { if (enabled) onClick() },
        modifier = modifier,
        backgroundColor = AppColor.Accent,
        focusedBackgroundColor = AppColor.Accent
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides AppColor.Black) {
                content()
            }
        }
    }
}

@Composable
fun TvTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        backgroundColor = Color.Transparent,
        focusedBackgroundColor = AppColor.SurfaceHover
    ) {
        Text(
            text = text,
            style = AppTypography.button,
            color = AppColor.OnSurface,
            modifier = Modifier.padding(horizontal = Spacing.md)
        )
    }
}

@Composable
fun TvIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
    contentDescription: String? = null,
    tint: Color = AppColor.OnSurface,
    content: @Composable (() -> Unit)? = null
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        backgroundColor = Color.Transparent,
        focusedBackgroundColor = AppColor.SurfaceHover
    ) {
        Box(contentAlignment = Alignment.Center) {
            content?.invoke() ?: imageVector?.let {
                Icon(imageVector = it, contentDescription = contentDescription, tint = tint)
            }
        }
    }
}

@Composable
fun TvOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false }
) {
    val interactionSource = remember { MutableInteractionSource() }
    val fieldModifier = modifier
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .tvFocusable(
            interactionSource = interactionSource,
            scale = Focus.scale,
            focusTarget = false
        )
        .onPreviewKeyEvent(onPreviewKeyEvent)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = fieldModifier,
        enabled = enabled,
        readOnly = false,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        interactionSource = interactionSource
    )
}
