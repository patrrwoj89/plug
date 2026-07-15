package com.polishmediahub.app.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.polishmediahub.app.R
import com.polishmediahub.app.data.local.ProfileEntity
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.screens.PinScreen
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Motion
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.ProfileViewModel

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
    SidebarItem(Screen.Music, R.string.music_title, Icons.Default.MusicNote),
    SidebarItem(Screen.Epg, R.string.epg_title, Icons.Default.LiveTv),
    SidebarItem(Screen.Torrents, R.string.torrents_title, Icons.Default.Download),
    SidebarItem(Screen.Downloads, R.string.downloads, Icons.Default.Download),
    SidebarItem(Screen.CustomLists, R.string.custom_lists_title, Icons.AutoMirrored.Filled.PlaylistPlay),
    SidebarItem(Screen.Settings, R.string.settings, Icons.Default.Settings),
    SidebarItem(Screen.Admin, R.string.sources, Icons.Default.AdminPanelSettings)
)

@Composable
fun Sidebar(
    current: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    showProfileHeader: Boolean = true
) {
    val expanded by remember { mutableStateOf(true) }
    val width by animateDpAsState(
        targetValue = if (expanded) 240.dp else 72.dp,
        animationSpec = tween(Motion.transition),
        label = "sidebar-width"
    )

    val profileViewModel = if (showProfileHeader) hiltViewModel<ProfileViewModel>() else null
    val currentProfile by profileViewModel?.currentProfile?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf<ProfileEntity?>(null) }
    val profiles by profileViewModel?.profiles?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(emptyList<ProfileEntity>()) }

    var showProfileDialog by remember { mutableStateOf(false) }
    var pinLockedProfile by remember { mutableStateOf<ProfileEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(AppColor.Surface)
            .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        item {
            ProfileHeader(
                profile = currentProfile,
                expanded = expanded,
                onClick = { showProfileDialog = true },
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
        }

        item {
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
        }

        item { Spacer(modifier = Modifier.height(Spacing.md)) }

        itemsIndexed(sidebarItems) { index, item ->
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

    if (showProfileDialog) {
        ProfileSelectionDialog(
            profiles = profiles,
            currentProfile = currentProfile,
            onSelect = { profile ->
                showProfileDialog = false
                if (profile.isPinLocked && profile.pinCode != null) {
                    pinLockedProfile = profile
                } else {
                    profileViewModel?.selectProfile(profile)
                }
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    pinLockedProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { pinLockedProfile = null },
            title = { Text(stringResource(id = R.string.enter_pin)) },
            text = {
                PinScreen(
                    onPinEntered = { pin ->
                        if (profileViewModel?.verifyPin(profile, pin) == true) {
                            pinLockedProfile = null
                            profileViewModel.selectProfile(profile)
                        }
                    },
                    onCancel = { pinLockedProfile = null }
                )
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@Composable
private fun ProfileHeader(
    profile: ProfileEntity?,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = profile?.name ?: stringResource(id = R.string.profile_default)

    FocusableSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        backgroundColor = AppColor.Surface,
        focusedBackgroundColor = AppColor.SurfaceHover
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColor.Accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (profile != null && !profile.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxHeight()
                    )
                } else {
                    Text(
                        text = name.take(1).uppercase(),
                        style = AppTypography.titleLarge,
                        color = AppColor.Accent
                    )
                }
            }
            if (expanded) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = AppTypography.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(id = R.string.profile_switch),
                        style = AppTypography.caption,
                        color = AppColor.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSelectionDialog(
    profiles: List<ProfileEntity>,
    currentProfile: ProfileEntity?,
    onSelect: (ProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.profile_select)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                profiles.forEach { profile ->
                    val selected = profile.id == currentProfile?.id
                    FocusableSurface(
                        onClick = { onSelect(profile) },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (selected) AppColor.Accent.copy(alpha = 0.2f) else AppColor.Surface,
                        focusedBackgroundColor = AppColor.SurfaceHover
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AppColor.Accent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.name.take(1).uppercase(),
                                    style = AppTypography.title,
                                    color = AppColor.Accent
                                )
                            }
                            Text(
                                text = profile.name + if (profile.isPinLocked) " 🔒" else "",
                                style = AppTypography.body,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            FocusableSurface(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), modifier = Modifier.padding(Spacing.md))
            }
        }
    )
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
