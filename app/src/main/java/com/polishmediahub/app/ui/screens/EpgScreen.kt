package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.polishmediahub.app.R
import com.polishmediahub.app.data.iptv.ChannelWithPrograms
import com.polishmediahub.app.data.local.EpgEntity
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.navigation.Screen
import com.polishmediahub.app.ui.components.EmptyState
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.TvOutlinedTextField
import com.polishmediahub.app.ui.theme.AppColor
import com.polishmediahub.app.ui.theme.AppTypography
import com.polishmediahub.app.ui.theme.Radius
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.EpgViewModel
import com.polishmediahub.app.ui.viewmodel.LastEpgSyncState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ChannelColumnWidth = 160.dp
private val RowHeight = 72.dp
private val PixelsPerMinute = 4f
private val MinutesPerSegment = 30
private val WindowHours = 4.5
private val LookBackMinutes = 30

@Composable
fun EpgScreen(
    modifier: Modifier = Modifier,
    channelId: String? = null,
    onNavigate: (Screen) -> Unit = {},
    viewModel: EpgViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lastEpgSync by viewModel.lastEpgSync.collectAsStateWithLifecycle()
    val timelineState by viewModel.timelineState.collectAsStateWithLifecycle()
    val channelsWithPrograms by viewModel.channelsWithPrograms.collectAsStateWithLifecycle()

    var url by remember { mutableStateOf(viewModel.epgUrl.value) }
    var m3uUrl by remember { mutableStateOf(viewModel.m3uUrl.value) }

    LaunchedEffect(Unit) {
        viewModel.m3uUrl.collect { m3uUrl = it }
    }
    LaunchedEffect(Unit) {
        viewModel.epgUrl.collect { url = it }
    }

    var selectedProgram by remember { mutableStateOf<EpgEntity?>(null) }
    var selectedChannel by remember { mutableStateOf<MediaItem?>(null) }
    val syncStatusText = remember(lastEpgSync) { formatEpgSyncStatus(context, lastEpgSync) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg)
    ) {
        Text(stringResource(id = R.string.epg_title), style = AppTypography.headline)
        Spacer(modifier = Modifier.height(Spacing.md))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            TvOutlinedTextField(
                value = m3uUrl,
                onValueChange = { m3uUrl = it },
                label = { Text(stringResource(id = R.string.epg_m3u_url)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            TvOutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(id = R.string.epg_url)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            FocusableSurface(
                onClick = {
                    if (m3uUrl.isNotBlank()) viewModel.loadChannels(m3uUrl)
                    if (url.isNotBlank()) viewModel.loadEpg(url)
                    viewModel.refresh()
                },
                modifier = Modifier.height(56.dp)
            ) {
                Text(stringResource(id = R.string.epg_refresh), modifier = Modifier.padding(horizontal = Spacing.md))
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = syncStatusText,
            style = AppTypography.caption,
            color = AppColor.OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (channelsWithPrograms.isEmpty()) {
            EmptyState(message = stringResource(id = R.string.epg_empty))
        } else {
            EpgTimelineGrid(
                now = timelineState.now,
                channelsWithPrograms = channelsWithPrograms,
                channelMap = timelineState.channels.associateBy { it.tvgId ?: it.id },
                onProgramFocused = { program, channel ->
                    selectedProgram = program
                    selectedChannel = channel
                },
                onProgramClick = { _, channel ->
                    channel?.let {
                        if (!it.videoUrl.isNullOrBlank()) onNavigate(Screen.Player(it.id))
                    }
                }
            )

            selectedProgram?.let { program ->
                ProgramDetailsPanel(program = program, now = timelineState.now, channel = selectedChannel)
            }
        }
    }
}

@Composable
private fun EpgTimelineGrid(
    now: Long,
    channelsWithPrograms: List<ChannelWithPrograms>,
    channelMap: Map<String, MediaItem>,
    onProgramFocused: (EpgEntity, MediaItem?) -> Unit,
    onProgramClick: (EpgEntity, MediaItem?) -> Unit
) {
    val windowStart = now - LookBackMinutes * 60 * 1000L
    val windowEnd = windowStart + (WindowHours * 60 * 60 * 1000).toLong()
    val totalMinutes = ((windowEnd - windowStart) / 60_000).toInt()
    val timelineWidth = (totalMinutes * PixelsPerMinute).dp
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        val nowMinutes = ((now - windowStart) / 60_000).toInt()
        val nowOffset = (nowMinutes * PixelsPerMinute).toInt()
        scrollState.animateScrollTo((nowOffset - 200).coerceAtLeast(0))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            EpgHeader(
                windowStart = windowStart,
                totalMinutes = totalMinutes,
                scrollState = scrollState
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(channelsWithPrograms, key = { it.channelId }) { channel ->
                    EpgChannelRow(
                        windowStart = windowStart,
                        windowEnd = windowEnd,
                        totalMinutes = totalMinutes,
                        timelineWidth = timelineWidth,
                        channel = channel,
                        channelMap = channelMap,
                        scrollState = scrollState,
                        now = now,
                        onProgramFocused = onProgramFocused,
                        onProgramClick = onProgramClick
                    )
                }
            }
        }

        val nowMinutes = ((now - windowStart) / 60_000).toInt()
        val nowOffset = (nowMinutes * PixelsPerMinute).dp
        val currentIndicatorOffset by remember { derivedStateOf { ChannelColumnWidth + nowOffset - scrollState.value.dp } }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .offset { IntOffset(currentIndicatorOffset.roundToPx(), 0) }
                .background(Color.Red)
        )
    }
}

@Composable
private fun EpgHeader(
    windowStart: Long,
    totalMinutes: Int,
    scrollState: ScrollState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(ChannelColumnWidth)) { }
        Row(modifier = Modifier.width((totalMinutes * PixelsPerMinute).dp)) {
            for (i in 0 until totalMinutes step MinutesPerSegment) {
                val time = windowStart + i * 60_000L
                Box(
                    modifier = Modifier.width((MinutesPerSegment * PixelsPerMinute).dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = formatTime(time),
                        style = AppTypography.caption,
                        color = AppColor.OnSurface,
                        modifier = Modifier.padding(start = Spacing.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun EpgChannelRow(
    windowStart: Long,
    windowEnd: Long,
    totalMinutes: Int,
    timelineWidth: Dp,
    channel: ChannelWithPrograms,
    channelMap: Map<String, MediaItem>,
    scrollState: ScrollState,
    now: Long,
    onProgramFocused: (EpgEntity, MediaItem?) -> Unit,
    onProgramClick: (EpgEntity, MediaItem?) -> Unit
) {
    val mediaItem = channelMap[channel.channelId]
    val sortedPrograms = channel.programs
        .filter { it.endTime > windowStart && it.startTime < windowEnd }
        .sortedBy { it.startTime }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChannelInfoBox(
            channelName = channel.channelName,
            logoUrl = mediaItem?.posterUrl,
            channelNumber = mediaItem?.channelNumber
        )
        Box(
            modifier = Modifier
                .width(timelineWidth)
                .height(RowHeight)
        ) {
            sortedPrograms.forEach { program ->
                ProgramTile(
                    program = program,
                    windowStart = windowStart,
                    windowEnd = windowEnd,
                    now = now,
                    channel = mediaItem,
                    onFocus = { onProgramFocused(program, mediaItem) },
                    onClick = { onProgramClick(program, mediaItem) }
                )
            }
        }
    }
}

@Composable
private fun ChannelInfoBox(
    channelName: String,
    logoUrl: String?,
    channelNumber: String?
) {
    Row(
        modifier = Modifier
            .width(ChannelColumnWidth)
            .height(RowHeight)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            .background(AppColor.SurfaceVariant, RoundedCornerShape(Radius.sm))
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logoUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (!channelNumber.isNullOrBlank()) {
                Text("$channelNumber", style = AppTypography.caption, color = AppColor.OnSurfaceVariant)
            }
            Text(
                text = channelName,
                style = AppTypography.body,
                color = AppColor.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProgramTile(
    program: EpgEntity,
    windowStart: Long,
    windowEnd: Long,
    now: Long,
    channel: MediaItem?,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val start = program.startTime.coerceAtLeast(windowStart)
    val end = program.endTime.coerceAtMost(windowEnd)
    val durationMs = (end - start).coerceAtLeast(60_000L)
    val offsetMinutes = ((start - windowStart) / 60_000).toInt()
    val durationMinutes = (durationMs / 60_000).toInt().coerceAtLeast(1)
    val width = (durationMinutes * PixelsPerMinute).dp
    val offset = (offsetMinutes * PixelsPerMinute).dp

    val isCurrent = now in program.startTime..program.endTime
    val progress = if (isCurrent && program.endTime > program.startTime) {
        ((now - program.startTime).toFloat() / (program.endTime - program.startTime)).coerceIn(0f, 1f)
    } else 0f

    FocusableSurface(
        onClick = onClick,
        modifier = Modifier
            .absoluteOffset(x = offset)
            .width(width)
            .height(RowHeight)
            .padding(1.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    onFocus()
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            },
        shape = RoundedCornerShape(Radius.sm),
        backgroundColor = if (isCurrent) AppColor.Accent.copy(alpha = 0.15f) else AppColor.SurfaceVariant,
        focusedBackgroundColor = AppColor.SurfaceHover
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xs)
                .drawBehind {
                    if (isCurrent) {
                        drawRect(
                            color = AppColor.Accent.copy(alpha = 0.3f),
                            size = size.copy(width = size.width * progress)
                        )
                    }
                },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = program.title,
                style = AppTypography.caption,
                color = AppColor.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatTime(program.startTime)} – ${formatTime(program.endTime)}",
                style = AppTypography.caption,
                color = AppColor.OnSurfaceVariant
            )
            if (isCurrent && progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColor.Accent,
                    trackColor = AppColor.SurfaceHover
                )
            }
        }
    }
}

@Composable
private fun ProgramDetailsPanel(
    program: EpgEntity,
    now: Long,
    channel: MediaItem?
) {
    val progress = if (now in program.startTime..program.endTime && program.endTime > program.startTime) {
        ((now - program.startTime).toFloat() / (program.endTime - program.startTime)).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColor.SurfaceVariant, RoundedCornerShape(Radius.md))
            .padding(Spacing.md)
    ) {
        Text(
            text = program.title,
            style = AppTypography.titleLarge,
            color = AppColor.OnSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = "${formatTime(program.startTime)} – ${formatTime(program.endTime)}",
                style = AppTypography.body,
                color = AppColor.OnSurfaceVariant
            )
            program.year?.let { Text(text = it, style = AppTypography.body, color = AppColor.OnSurfaceVariant) }
            program.category?.let { Text(text = it, style = AppTypography.body, color = AppColor.OnSurfaceVariant) }
        }
        if (program.description.isNotBlank()) {
            Text(
                text = program.description,
                style = AppTypography.body,
                color = AppColor.OnSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Spacing.sm)
            )
        }
        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm),
                color = AppColor.Accent,
                trackColor = AppColor.SurfaceHover
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = AppTypography.caption,
                color = AppColor.OnSurfaceVariant
            )
        }
    }
}

private fun formatEpgSyncStatus(context: android.content.Context, state: LastEpgSyncState): String {
    val date = if (state.at > 0L) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(state.at))
    } else {
        context.getString(R.string.epg_status_never)
    }
    val status = when (state.status) {
        "success" -> context.getString(R.string.epg_status_success)
        "error" -> context.getString(R.string.epg_status_error) + (state.error?.let { " ($it)" } ?: "")
        else -> if (state.at > 0L) state.status else ""
    }
    return context.getString(R.string.epg_last_sync, date, status)
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
