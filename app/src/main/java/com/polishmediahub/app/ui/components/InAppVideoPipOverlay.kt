package com.polishmediahub.app.ui.components

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.polishmediahub.app.navigation.LocalPlayerViewModel
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.PlayerViewModel

@Composable
fun InAppVideoPipOverlay(
    onNavigate: (Screen) -> Unit,
    currentScreen: Screen,
    viewModel: PlayerViewModel = LocalPlayerViewModel.current
) {
    val manager = viewModel.videoPipManager
    val isInPipMode by manager.isInPipMode.collectAsStateWithLifecycle()
    val currentItem by manager.currentMediaItem.collectAsStateWithLifecycle()
    val exoPlayer by manager.exoPlayer.collectAsStateWithLifecycle()

    val visible = isInPipMode && exoPlayer != null && currentItem != null && currentScreen !is Screen.Player

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.fillMaxSize()
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier
                    .padding(Spacing.lg)
                    .width(360.dp)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(AppColor.Surface.copy(alpha = 0.95f))
                    .border(1.dp, AppColor.OnSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(Radius.md))
                    .focusRequester(focusRequester)
                    .focusable(true)
                    .onPreviewKeyEvent { keyEvent ->
                        val native = keyEvent.nativeKeyEvent
                        if (native.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                        when (native.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_BUTTON_A -> {
                                manager.exitPip()
                                currentItem?.let { onNavigate(Screen.Player(it.id)) }
                                true
                            }
                            else -> false
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 360.dp, height = 202.dp)
                        .clip(RoundedCornerShape(Radius.md))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                            }
                        },
                        update = { view ->
                            view.player = exoPlayer
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AppColor.OnSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = currentItem?.title ?: "",
                            style = com.polishmediahub.app.ui.theme.AppTypography.body,
                            color = AppColor.OnSurface,
                            maxLines = 1
                        )
                    }
                    TvIconButton(
                        onClick = { manager.stopAndRelease() },
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop"
                    )
                }
            }
        }

        DisposableEffect(Unit) {
            manager.enterPip()
            onDispose { }
        }
    }
}
