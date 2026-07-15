package com.polishmediahub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import com.polishmediahub.app.R
import com.polishmediahub.app.data.local.ProfileEntity
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing

internal val SIDEBAR_AUTO_COLLAPSE_DELAY_MS = 1500L
internal val SIDEBAR_OPEN_WIDTH = 260.dp
internal val SIDEBAR_PILL_HEIGHT = 48.dp

@Composable
internal fun CollapsedSidebarPill(
    current: Screen,
    profile: ProfileEntity?,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val currentItem = remember(current) { sidebarItems.firstOrNull { it.screen == current } ?: sidebarItems.first() }

    Box(
        modifier = modifier
            .height(SIDEBAR_PILL_HEIGHT)
            .wrapContentWidth()
            .clip(RoundedCornerShape(Radius.round))
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = AppColor.Surface.copy(alpha = 0.6f),
                    tints = listOf(HazeTint(AppColor.Surface.copy(alpha = 0.3f))),
                    blurRadius = 16.dp,
                    noiseFactor = 0f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(horizontal = Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppColor.Accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                val avatar = profile?.avatarUrl
                val avatarDescription = profile?.name ?: stringResource(id = R.string.profile_default)
                if (!avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = avatar,
                        contentDescription = avatarDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = (profile?.name ?: stringResource(id = R.string.profile_default)).take(1).uppercase(),
                        style = AppTypography.title,
                        color = AppColor.Accent
                    )
                }
            }

            Icon(
                imageVector = currentItem.icon,
                contentDescription = stringResource(id = currentItem.labelRes),
                tint = AppColor.OnSurface,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = stringResource(id = currentItem.labelRes),
                style = AppTypography.button,
                color = AppColor.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ModernSidebarBlurPanel(
    current: Screen,
    profile: ProfileEntity?,
    hazeState: HazeState,
    focusRequester: FocusRequester,
    onProfileClick: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstItemRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstItemRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .width(SIDEBAR_OPEN_WIDTH)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = Radius.xl, bottomEnd = Radius.xl))
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = AppColor.Surface.copy(alpha = 0.75f),
                    tints = listOf(HazeTint(AppColor.Surface.copy(alpha = 0.4f))),
                    blurRadius = 24.dp,
                    noiseFactor = 0.05f
                )
            )
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    onInteraction()
                }
                false
            },
        contentAlignment = Alignment.TopStart
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            item {
                ProfileHeader(
                    profile = profile,
                    expanded = true,
                    onClick = onProfileClick,
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = Spacing.md),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = AppTypography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(Spacing.md)) }

            sidebarGroups.forEachIndexed { groupIndex, group ->
                item {
                    SidebarSectionHeader(
                        labelRes = group.labelRes,
                        modifier = Modifier.padding(horizontal = Spacing.md)
                    )
                }
                itemsIndexed(group.items) { itemIndex, item ->
                    SidebarRow(
                        item = item,
                        selected = current == item.screen,
                        expanded = true,
                        onClick = { onNavigate(item.screen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.sm)
                            .then(if (groupIndex == 0 && itemIndex == 0) Modifier.focusRequester(firstItemRequester) else Modifier)
                    )
                }
                item { Spacer(modifier = Modifier.height(Spacing.xs)) }
            }
        }
    }
}
