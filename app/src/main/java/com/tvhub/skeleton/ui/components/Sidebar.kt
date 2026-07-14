package com.tvhub.skeleton.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tvhub.skeleton.R
import com.tvhub.skeleton.navigation.Screen
import com.tvhub.skeleton.ui.theme.AppColor
import com.tvhub.skeleton.ui.theme.AppTypography
import com.tvhub.skeleton.ui.theme.Motion
import com.tvhub.skeleton.ui.theme.Spacing

data class SidebarItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector
)

private val sidebarItems = listOf(
    SidebarItem(Screen.Home, R.string.home, Icons.Default.Home),
    SidebarItem(Screen.Search, R.string.search, Icons.Default.Search),
    SidebarItem(Screen.Library, R.string.library, Icons.AutoMirrored.Filled.LibraryBooks),
    SidebarItem(Screen.Watchlist, R.string.watchlist, Icons.Default.WatchLater),
    SidebarItem(Screen.Anime, R.string.anime, Icons.Default.Animation),
    SidebarItem(Screen.Downloads, R.string.downloads, Icons.Default.Download),
    SidebarItem(Screen.Settings, R.string.settings, Icons.Default.Settings),
    SidebarItem(Screen.Admin, R.string.sources, Icons.Default.AdminPanelSettings)
)

@Composable
fun Sidebar(
    current: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val expanded by remember { mutableStateOf(true) }
    val width by animateDpAsState(
        targetValue = if (expanded) 240.dp else 72.dp,
        animationSpec = tween(Motion.transition),
        label = "sidebar-width"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(AppColor.Surface)
            .padding(vertical = Spacing.md)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        // Logo area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = Spacing.md),
            contentAlignment = Alignment.CenterStart
        ) {
            if (expanded) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = AppTypography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = stringResource(id = R.string.app_name),
                    tint = AppColor.Accent,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        sidebarItems.forEachIndexed { index, item ->
            val isFirst = index == 0
            SidebarRow(
                item = item,
                selected = current == item.screen,
                expanded = expanded,
                onClick = { onNavigate(item.screen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm)
                    .then(if (isFirst) Modifier.focusRequester(focusRequester) else Modifier)
            )
        }
    }
}

@Composable
private fun SidebarRow(
    item: SidebarItem,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) AppColor.Accent.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (selected) AppColor.Accent else AppColor.OnSurfaceVariant
    val label = stringResource(id = item.labelRes)

    FocusableSurface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        scale = 1.02f,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        backgroundColor = containerColor,
        focusedBackgroundColor = AppColor.SurfaceHover
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            if (expanded) {
                Text(
                    text = label,
                    style = AppTypography.button,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
