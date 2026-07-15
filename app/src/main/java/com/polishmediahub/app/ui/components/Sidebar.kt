package com.polishmediahub.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.polishmediahub.app.R
import com.polishmediahub.app.data.local.ProfileEntity
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.screens.PinScreen
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

internal data class SidebarItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector
)

internal data class SidebarGroup(
    val labelRes: Int,
    val items: List<SidebarItem>
)

internal val sidebarGroups = listOf(
    SidebarGroup(R.string.sidebar_section_discover, listOf(
        SidebarItem(Screen.Home, R.string.home, Icons.Default.Home),
        SidebarItem(Screen.Search, R.string.search, Icons.Default.Search)
    )),
    SidebarGroup(R.string.sidebar_section_library, listOf(
        SidebarItem(Screen.Library, R.string.library, Icons.AutoMirrored.Filled.LibraryBooks),
        SidebarItem(Screen.Watchlist, R.string.watchlist, Icons.Default.WatchLater),
        SidebarItem(Screen.CustomLists, R.string.custom_lists_title, Icons.AutoMirrored.Filled.PlaylistPlay)
    )),
    SidebarGroup(R.string.sidebar_section_multimedia, listOf(
        SidebarItem(Screen.Epg, R.string.epg_title, Icons.Default.LiveTv),
        SidebarItem(Screen.Anime, R.string.anime, Icons.Default.Animation),
        SidebarItem(Screen.Music, R.string.music_title, Icons.Default.MusicNote)
    )),
    SidebarGroup(R.string.sidebar_section_downloads, listOf(
        SidebarItem(Screen.Torrents, R.string.torrents_title, Icons.Default.Download),
        SidebarItem(Screen.Downloads, R.string.downloads, Icons.Default.Download)
    )),
    SidebarGroup(R.string.sidebar_section_system, listOf(
        SidebarItem(Screen.Settings, R.string.settings, Icons.Default.Settings),
        SidebarItem(Screen.Admin, R.string.sources, Icons.Default.AdminPanelSettings)
    ))
)

internal val sidebarItems = sidebarGroups.flatMap { it.items }

@Composable
fun Sidebar(
    current: Screen,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    hazeState: HazeState,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    profile: ProfileEntity? = null,
    profiles: List<ProfileEntity> = emptyList(),
    onSelectProfile: (ProfileEntity) -> Unit = {},
    onVerifyPin: (ProfileEntity, String) -> Boolean = { _, _ -> false }
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var pinLockedProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    val panelFocusRequester = remember { FocusRequester() }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(expanded) {
        if (!expanded) return@LaunchedEffect
        lastInteraction = System.currentTimeMillis()
        while (expanded) {
            delay(100)
            if (System.currentTimeMillis() - lastInteraction >= SIDEBAR_AUTO_COLLAPSE_DELAY_MS) {
                onExpandedChange(false)
                break
            }
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(50)
            panelFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(SIDEBAR_OPEN_WIDTH),
        contentAlignment = Alignment.TopStart
    ) {
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CollapsedSidebarPill(
                current = current,
                profile = profile,
                hazeState = hazeState,
                modifier = Modifier.padding(Spacing.md)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ModernSidebarBlurPanel(
                current = current,
                profile = profile,
                hazeState = hazeState,
                focusRequester = panelFocusRequester,
                onProfileClick = { showProfileDialog = true },
                onNavigate = { screen ->
                    onExpandedChange(false)
                    onNavigate(screen)
                },
                onInteraction = { lastInteraction = System.currentTimeMillis() },
                modifier = Modifier.fillMaxHeight()
            )
        }
    }

    if (showProfileDialog) {
        ProfileSelectionDialog(
            profiles = profiles,
            currentProfile = profile,
            onSelect = { selected ->
                showProfileDialog = false
                if (selected.isPinLocked && selected.pinCode != null) {
                    pinLockedProfile = selected
                } else {
                    onSelectProfile(selected)
                }
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    pinLockedProfile?.let { lockedProfile ->
        AlertDialog(
            onDismissRequest = { pinLockedProfile = null },
            title = { Text(stringResource(id = R.string.enter_pin)) },
            text = {
                PinScreen(
                    onPinEntered = { pin ->
                        if (onVerifyPin(lockedProfile, pin)) {
                            pinLockedProfile = null
                            onSelectProfile(lockedProfile)
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
internal fun ProfileHeader(
    profile: ProfileEntity?,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = profile?.name ?: stringResource(id = R.string.profile_default)

    FocusableSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
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
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
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
internal fun ProfileSelectionDialog(
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
                    val profileDescription = profile.name + if (selected) ", " + stringResource(id = R.string.state_selected) else ""
                    FocusableSurface(
                        onClick = { onSelect(profile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = profileDescription },
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = profile.name,
                                    style = AppTypography.body,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (profile.isPinLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = stringResource(id = R.string.profile_locked),
                                        tint = AppColor.OnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
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
internal fun SidebarSectionHeader(
    labelRes: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(id = labelRes),
        style = AppTypography.caption,
        color = AppColor.OnSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    )
}

@Composable
internal fun SidebarRow(
    item: SidebarItem,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) AppColor.Accent.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (selected) AppColor.Accent else AppColor.OnSurfaceVariant
    val label = stringResource(id = item.labelRes)
    val selectionState = stringResource(id = if (selected) R.string.state_selected else R.string.state_not_selected)

    FocusableSurface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .semantics { contentDescription = "$label, $selectionState" },
        scale = 1.02f,
        shape = RoundedCornerShape(8.dp),
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
